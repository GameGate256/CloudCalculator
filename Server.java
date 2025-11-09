import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.concurrent.*;

// tcp 계산기 서버 구현
// 요청 한 줄 수신 후 응답 한 줄 송신
// 다중 클라이언트 처리는 스레드 풀 사용
public class Server {

    private static final int DEFAULT_PORT = 1234;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        // 숫자 포맷 일관성 확보
        Locale.setDefault(Locale.US);

        // 포트 결정
        int port = DEFAULT_PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignore) { /* keep default */ }
        }

        // 스레드 풀 크기 설정
        int nThreads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);

        // 서버 소켓 바인드 및 accept 루프 시작
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[Server] Listening on " + port);

            // 안전 종료 훅
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running = false;
                try { ss.close(); } catch (IOException ignored) {}
                pool.shutdown();
                System.out.println("[Server] Shutdown complete");
            }));

            // 연결 수락 루프
            while (running) {
                try {
                    Socket s = ss.accept();
                    // 타임아웃 비활성화
                    s.setSoTimeout(0);
                    // 클라이언트 연결을 작업으로 위임
                    pool.submit(new ClientHandler(s));
                } catch (SocketException se) {
                    // 서버 소켓 종료 시 루프 탈출
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[Server] Bind failed: " + e.getMessage());
        } finally {
            // 풀 정리
            pool.shutdown();
        }
    }

    // 개별 클라이언트 연결 담당 작업자
    // 소켓 하나에 대응
    private static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            String remote = socket.getRemoteSocketAddress().toString();
            System.out.println("[Server] Connected: " + remote);

            // 텍스트 기반 입출력 스트림 준비
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"))) {

                String line;
                // 요청 단위는 개행으로 구분된 한 줄
                while ((line = in.readLine()) != null) {
                    // 공백 라인은 건너뛴다
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    // 종료 요청 처리
                    if (line.equalsIgnoreCase("QUIT")) {
                        write(out, "RESP 200 OK result=bye");
                        break;
                    }
                    // 생존 확인 처리
                    if (line.equalsIgnoreCase("PING")) {
                        write(out, "RESP 204 NO_CONTENT");
                        continue;
                    }

                    // 계산 요청 처리
                    write(out, process(line));
                }
            } catch (IOException e) {
                System.out.println("[Server] I/O error " + remote + ": " + e.getMessage());
            } finally {
                // 자원 해제
                try { socket.close(); } catch (IOException ignored) {}
                System.out.println("[Server] Disconnected: " + remote);
            }
        }

        // 한 줄 응답 전송
        private void write(BufferedWriter out, String msg) throws IOException {
            out.write(msg);
            out.write("\r\n");
            out.flush();
        }

        // 요청 파싱과 연산 실행
        // 지원 형식은 OP a b
        private String process(String req) {
            String[] t = req.split("\\s+");
            if (t.length == 0) return err("FORMAT_ERROR", "empty request");

            String op = t[0].toUpperCase(Locale.ROOT);

            // 사칙 연산 분기
            switch (op) {
                case "ADD":
                case "SUB":
                case "MUL":
                case "DIV":
                    // 인자 수 검증
                    if (t.length != 3) return err("ARITY_ERROR", "expected 2 arguments");

                    // 숫자 파싱
                    Double a = parse(t[1]);
                    Double b = parse(t[2]);
                    if (a == null || b == null) return err("FORMAT_ERROR", "number format");

                    // 나눗셈 제로 검증
                    if ("DIV".equals(op) && b == 0.0) return err("DIV_BY_ZERO", "divided by zero");

                    // 연산 실행
                    double r;
                    switch (op) {
                        case "ADD": r = a + b; break;
                        case "SUB": r = a - b; break;
                        case "MUL": r = a * b; break;
                        case "DIV": r = a / b; break;
                        default:    return err("INVALID_OP", "unsupported operation");
                    }
                    // 성공 응답
                    return ok(r);

                // 미지원 연산
                default:
                    return err("INVALID_OP", "unsupported operation");
            }
        }

        // 문자열을 실수로 파싱 실패 시 null 반환
        private Double parse(String s) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return null; }
        }

        // 성공 응답 생성
        private String ok(double v) {
            return "RESP 200 OK result=" + format(v);
        }

        // 오류 응답 생성
        private String err(String type, String msg) {
            return "RESP 400 ERROR type=" + type + " message=" + sanitize(msg);
        }

        // 개행과 탭을 공백으로 정리
        private String sanitize(String m) {
            if (m == null) return "";
            return m.replaceAll("[\\r\\n\\t]+", " ").trim();
        }

        // 사람이 읽기 좋은 숫자 포맷
        // 정수면 소수부 제거
        private String format(double d) {
            if (Math.floor(d) == d && Math.abs(d) < 9_223_372_036_854_775L) {
                return String.valueOf((long) d);
            }
            String s = String.format(Locale.US, "%.12f", d);
            return s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }
}
