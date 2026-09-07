# JForex Tick Exporter

A [JForex / Dukascopy](https://www.dukascopy.com/swiss/english/forex/jforex/) strategy that exports historical tick data (Bid / Ask / volumes) to CSV — packaged with a standalone runner and a **GitHub Actions** workflow, so the export runs in the cloud with no local JForex installation.

## Project structure

```
├── .github/workflows/
│   ├── tick-export.yml   # manual export run (workflow_dispatch) + CSV artifact upload
│   └── build.yml         # compile check on every push (no credentials needed)
├── src/main/java/
│   ├── TickExporter.java    # the strategy (chunked IHistory.getTicks -> CSV)
│   └── StrategyRunner.java  # runs the strategy standalone via the JForex API
├── scripts/
│   ├── download-deps.sh  # downloads jforexlib.jar into lib/
│   ├── build.sh          # compiles into bin/
│   └── run.sh            # runs StrategyRunner
└── output/               # CSV output (created at runtime)
```

## GitHub Actions setup

1. **Create a repo and push this project:**
   ```bash
   git init && git add . && git commit -m "Initial commit"
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. **Register a free Dukascopy DEMO account** at
   https://www.dukascopy.com/swiss/english/forex/demo/
   You receive a username and password by email (demo accounts are valid ~14 days and can be renewed).
3. In the repo go to **Settings → Secrets and variables → Actions** and add two secrets:
   - `JFOREX_USERNAME` — the demo login
   - `JFOREX_PASSWORD` — the demo password
4. Go to **Actions → JForex Tick Export → Run workflow**, pick the instrument and date range, and start it.
5. When the run finishes, download the CSV from the **Artifacts** section of that run.

## Configuration (environment variables)

| Variable | Default | Description |
|---|---|---|
| `JFOREX_INSTRUMENT` | `EURUSD` | Instrument, e.g. `EURUSD`, `GBPUSD`, `XAUUSD` |
| `JFOREX_FROM` | `2010-01-01 00:00:00` | Range start, **GMT** |
| `JFOREX_TO` | `2020-01-01 00:00:00` | Range end, **GMT** |
| `JFOREX_OUT_FILE` | `output/<INSTRUMENT>_ticks.csv` | Output CSV path |
| `JFOREX_CHUNK_HOURS` | `6` | Hours downloaded per history request |
| `JFOREX_SLEEP_MS` | `500` | Pause between requests (politeness) |
| `JFOREX_USERNAME` / `JFOREX_PASSWORD` | — | Dukascopy credentials |
| `JFOREX_JNLP` | demo JNLP | Server entry point (demo by default) |

## Run locally

```bash
bash scripts/download-deps.sh     # downloads jforexlib.jar into ./lib
bash scripts/build.sh             # compiles into ./bin
export JFOREX_USERNAME=... JFOREX_PASSWORD=...
bash scripts/run.sh
```

You can also still drop `TickExporter.java` into the JForex client and run it as a normal
strategy — when no environment variables are set, the built-in defaults are used.

## Limitations & tips

- **GitHub-hosted jobs are limited to ~6 hours.** Ten years of EURUSD ticks is hundreds of
  millions of rows — split big ranges into month/quarter-sized runs and download each artifact.
- Tick CSVs get **large** (a busy year of EURUSD can be several GB uncompressed).
- If `jforexlib.jar` cannot be auto-downloaded, grab the JForex SDK from Dukascopy and place
  the jar in `lib/` manually.
- All timestamps are **GMT**, as returned by the JForex API.

---

## راهنمای سریع (فارسی)

این پروژه استراتژی `TickExporter` را طوری اجرا می‌کند که نیازی به نصب JForex روی سیستم خودتان
نباشد؛ همه‌چیز روی GitHub Actions اجرا می‌شود:

1. پروژه را در یک ریپوی گیت‌هاب push کنید.
2. یک حساب **دموی رایگان دوکاسکپی** بسازید و نام کاربری/رمز را در Secrets ریپو با نام‌های
   `JFOREX_USERNAME` و `JFOREX_PASSWORD` ذخیره کنید.
3. از تب **Actions** ورک‌فلوی **JForex Tick Export** را با دکمه‌ی **Run workflow** اجرا کنید
   و نماد و بازه‌ی زمانی را انتخاب کنید.
4. بعد از اتمام اجرا، فایل CSV را از بخش **Artifacts** همان اجرا دانلود کنید.

⚠️ هر اجرای GitHub حداکثر حدود ۶ ساعت طول می‌کشد؛ بازه‌های بزرگ (مثلاً ۱۰ سال) را به اجراهای
چندماهه تقسیم کنید و خروجی هر اجرا را جداگانه دانلود کنید.

## License

MIT — see [LICENSE](LICENSE).
