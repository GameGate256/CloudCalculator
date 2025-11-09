import java.io.*;
import java.net.Socket;

// 통합 스모크 테스트 실행기
// 서버를 데몬 스레드로 띄우고 표준 시나리오를 순차 실행
public class Main {
    public static void main(String[] args) throws Exception {
        // 서버 데몬 스레드 실행
        Thread server = new Thread(() -> Server.main(new String[]{"1234"}));
        server.setDaemon(true);
        server.start();

        // 서버 기동 대기
        Thread.sleep(1000);

        System.out.println("[Main] Client test start");
        runClientSmokeTest();
        System.out.println("[Main] Client test end");
    }

    // 기본 명령 흐름을 순차 실행
    private static void runClientSmokeTest() {
        try {
            // 설정 로드
            Config cfg = Config.loadOrCreateDefault("server_info.dat");

            // 단일 연결로 여러 명령 수행
            try (Socket s = new Socket(cfg.host, cfg.port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"))) {

                String[] cmds = {
                        "PING",
                        "QUIT"
                };

                // 요청 전송과 응답 출력
                for (String c : cmds) {
                    System.out.println("> " + c);
                    out.write(c);
                    out.write("\r\n");
                    out.flush();

                    String resp = in.readLine();
                    if (resp == null) break;
                    System.out.println(resp);
                }
            }
        } catch (IOException e) {
            System.err.println("[Main] Test I/O error: " + e.getMessage());
        }
    }
}
