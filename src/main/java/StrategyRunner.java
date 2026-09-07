import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.IClientFactory;

import java.util.concurrent.CountDownLatch;

/**
 * StrategyRunner — اجرای استراتژی TickExporter بدون نصب پلتفرم JForex
 * (حالت standalone با استفاده از JForex API).
 *
 * متغیرهای محیطی لازم:
 *   JFOREX_USERNAME   نام کاربری حساب (دمو یا لایو) دوکاسکپی
 *   JFOREX_PASSWORD   رمز عبور
 *   JFOREX_JNLP       (اختیاری) آدرس JNLP سرور — پیش‌فرض: سرور دمو
 */
public class StrategyRunner {

    private static final String DEFAULT_JNLP =
            "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";

    public static void main(String[] args) throws Exception {
        String username = requiredEnv("JFOREX_USERNAME");
        String password = requiredEnv("JFOREX_PASSWORD");
        String jnlp = System.getenv().getOrDefault("JFOREX_JNLP", DEFAULT_JNLP);

        System.out.println("[Runner] Connecting to JForex (" + jnlp + ") as " + username);
        IClient client = IClientFactory.getDefaultInstance();
        client.connect(jnlp, username, password); // متد synchronous — تا لاگین منتظر می‌ماند
        System.out.println("[Runner] Connected. Starting TickExporter...");

        CountDownLatch done = new CountDownLatch(1);
        long id = client.startStrategy(new TickExporter(done));
        System.out.println("[Runner] Strategy started, id=" + id);

        done.await(); // TickExporter در onStop() آن را آزاد می‌کند

        System.out.println("[Runner] Strategy finished. Disconnecting...");
        client.stop();
        System.out.println("[Runner] Done.");
        System.exit(0);
    }

    private static String requiredEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.trim().isEmpty()) {
            System.err.println("[Runner] Missing environment variable: " + name);
            System.exit(2);
        }
        return v.trim();
    }
}
