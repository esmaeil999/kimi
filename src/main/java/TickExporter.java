import com.dukascopy.api.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

/**
 * TickExporter — استراتژی JForex برای خروجی گرفتن تیک‌های تاریخی به CSV.
 *
 * همه‌ی تنظیمات از طریق Environment Variable قابل تغییر است (برای GitHub Actions).
 * اگر متغیری ست نشده باشد، مقدار پیش‌فرض زیر استفاده می‌شود.
 *
 *   JFOREX_INSTRUMENT   مثل EURUSD یا EUR/USD        (پیش‌فرض: EURUSD)
 *   JFOREX_FROM         "yyyy-MM-dd HH:mm:ss" GMT    (پیش‌فرض: 2010-01-01 00:00:00)
 *   JFOREX_TO           "yyyy-MM-dd HH:mm:ss" GMT    (پیش‌فرض: 2020-01-01 00:00:00)
 *   JFOREX_OUT_FILE     مسیر فایل خروجی CSV          (پیش‌فرض: output/EURUSD_ticks.csv)
 *   JFOREX_CHUNK_HOURS  ساعت‌های هر درخواست           (پیش‌فرض: 6)
 *   JFOREX_SLEEP_MS     مکث بین درخواست‌ها (ms)      (پیش‌فرض: 500)
 */
public class TickExporter implements IStrategy {

    // ================== تنظیمات پیش‌فرض ==================
    private static final String DEF_INSTRUMENT = "EURUSD";
    private static final String DEF_FROM_STR   = "2010-01-01 00:00:00";
    private static final String DEF_TO_STR     = "2020-01-01 00:00:00";
    private static final long   DEF_SLEEP_MS   = 500;
    // ======================================================

    private final CountDownLatch doneLatch; // برای اجرای standalone (StrategyRunner)
    private IContext context;
    private IHistory history;
    private IConsole console;

    /** سازنده‌ی پیش‌فرض — برای اجرا داخل پلتفرم JForex */
    public TickExporter() {
        this(null);
    }

    /** سازنده‌ی standalone — StrategyRunner با latch منتظر پایان می‌ماند */
    public TickExporter(CountDownLatch doneLatch) {
        this.doneLatch = doneLatch;
    }

    @Override
    public void onStart(IContext context) throws JFException {
        this.context = context;
        this.history = context.getHistory();
        this.console = context.getConsole();

        final Instrument instrument = parseInstrument(env("JFOREX_INSTRUMENT", DEF_INSTRUMENT));
        final String fromStr = env("JFOREX_FROM", DEF_FROM_STR);
        final String toStr   = env("JFOREX_TO",   DEF_TO_STR);
        final String outFile = env("JFOREX_OUT_FILE",
                "output/" + instrument.toString().replace("/", "") + "_ticks.csv");
        final long chunkMs   = Long.parseLong(env("JFOREX_CHUNK_HOURS", "6")) * 60L * 60 * 1000;
        final long sleepMs   = Long.parseLong(env("JFOREX_SLEEP_MS", String.valueOf(DEF_SLEEP_MS)));

        PrintWriter out = null;
        try {
            // همه‌ی زمان‌ها در JForex بر مبنای GMT است
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            parser.setTimeZone(TimeZone.getTimeZone("GMT"));
            long from = parser.parse(fromStr).getTime();
            long to   = parser.parse(toStr).getTime();

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            fmt.setTimeZone(TimeZone.getTimeZone("GMT"));

            File file = new File(outFile);
            if (file.getParentFile() != null) file.getParentFile().mkdirs();

            // BufferedWriter برای نوشتن سریع میلیون‌ها خط
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)));
            out.println("GmtTime,Bid,Ask,BidVolume,AskVolume");

            console.getOut().println("Exporting " + instrument + " ticks: "
                    + fromStr + " -> " + toStr + " (GMT)");
            long startedAt = System.currentTimeMillis();

            long cursor = from;
            long lastTickTime = -1;
            long total = 0;

            while (cursor < to) {
                long chunkEnd = Math.min(cursor + chunkMs, to);

                // دانلود تیک‌های بازه‌ی [cursor تا chunkEnd] — این متد synchronous است
                List<ITick> ticks = history.getTicks(instrument, cursor, chunkEnd);

                for (ITick t : ticks) {
                    if (t.getTime() <= lastTickTime) continue; // حذف تیک تکراری مرز چانک‌ها
                    lastTickTime = t.getTime();

                    out.println(fmt.format(new Date(t.getTime())) + ","
                            + t.getBid() + ","
                            + t.getAsk() + ","
                            + t.getBidVolume() + ","
                            + t.getAskVolume());
                    total++;
                }
                out.flush();

                console.getOut().println("chunk: " + fmt.format(new Date(chunkEnd))
                        + " | ticks: " + ticks.size() + " | total: " + total);

                cursor = chunkEnd;
                if (sleepMs > 0) Thread.sleep(sleepMs); // استراحت کوتاه بین درخواست‌ها
            }

            long elapsed = (System.currentTimeMillis() - startedAt) / 1000;
            console.getOut().println("FINISHED. total ticks = " + total
                    + " | elapsed = " + elapsed + "s | file = " + file.getAbsolutePath());

        } catch (Exception e) {
            console.getErr().println("Error: " + e);
        } finally {
            if (out != null) out.close();
            context.stop(); // توقف استراتژی در هر صورت (موفقیت یا خطا)
        }
    }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    private static Instrument parseInstrument(String s) {
        return Instrument.valueOf(s.replace("/", "").replace("_", "").toUpperCase());
    }

    @Override public void onTick(Instrument instrument, ITick tick) {}
    @Override public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {}
    @Override public void onMessage(IMessage message) {}
    @Override public void onAccount(IAccount account) {}

    @Override
    public void onStop() {
        if (doneLatch != null) doneLatch.countDown(); // آزادسازی StrategyRunner
    }
}
