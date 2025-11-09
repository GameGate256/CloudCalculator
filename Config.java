import java.io.*;
import java.util.Properties;

// 서버 접속 설정 담당
// 파일이 없으면 기본값으로 생성하고 저장
public class Config {
    public final String host;
    public final int port;

    public Config(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // 설정 파일을 읽고 유효하지 않으면 기본값 사용
    public static Config loadOrCreateDefault(String path) {
        Properties p = new Properties();
        File f = new File(path);

        String host = "localhost";
        int port = 1234;

        if (f.exists()) {
            // 기존 파일 로드
            try (FileInputStream fis = new FileInputStream(f);
                 InputStreamReader r = new InputStreamReader(fis, "UTF-8")) {
                p.load(r);
                host = p.getProperty("host", host).trim();
                try { port = Integer.parseInt(p.getProperty("port", String.valueOf(port)).trim()); }
                catch (NumberFormatException ignore) {}
            } catch (IOException e) {
                System.err.println("[Config] Read failed: " + e.getMessage());
            }
        } else {
            // 기본 파일 생성
            try (FileOutputStream fos = new FileOutputStream(f);
                 OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8")) {
                w.write("host=" + host + "\n");
                w.write("port=" + port + "\n");
                w.flush();
                System.out.println("[Config] Created default " + path + " (" + host + ":" + port + ")");
            } catch (IOException e) {
                System.err.println("[Config] Create failed: " + e.getMessage());
            }
        }
        return new Config(host, port);
    }
}
