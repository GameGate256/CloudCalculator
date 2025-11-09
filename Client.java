import java.io.*;
import java.net.Socket;
import java.util.Locale;

// 콘솔 기반 동기 클라이언트
// 사용자 입력 한 줄을 서버에 전송하고 즉시 한 줄 응답을 출력
public class Client {

    public static void main(String[] args) {
        // 숫자 포맷 일관성 확보
        Locale.setDefault(Locale.US);

        // 설정 로드 없으면 기본값 생성
        Config cfg = Config.loadOrCreateDefault("server_info.dat");
        System.out.println("[Client] Server: " + cfg.host + ":" + cfg.port);

        // 서버와 단일 연결 생성
        try (Socket socket = new Socket(cfg.host, cfg.port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            // 사용 가능한 명령 안내
            printHelp();

            // 입력 루프 시작
            while (true) {
                System.out.print("> ");
                String line = console.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                // 요청 전송
                out.write(line);
                out.write("\r\n");
                out.flush();

                // 응답 수신
                String resp = in.readLine();
                if (resp == null) {
                    System.out.println("(server closed)");
                    break;
                }
                System.out.println(resp);

                // 종료 명령이면 루프 탈출
                if ("QUIT".equalsIgnoreCase(line)) break;
            }
        } catch (IOException e) {
            System.err.println("[Client] I/O error: " + e.getMessage());
        }
    }

    // 명령 요약 출력
    private static void printHelp() {
        System.out.println("=== Cloud Calculator Client ===");
        System.out.println("ADD a b | SUB a b | MUL a b | DIV a b | PING | QUIT");
        System.out.println();
    }
}
