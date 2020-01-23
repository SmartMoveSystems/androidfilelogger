# androidfilelogger
SmartMove Android File Logger. Redirects Timber logging to a rolling set of log files. Allows 
automatic logging of crashes to a configured HTTP endpoint.

## Including in your project

In your project-level build.gradle:

```
allprojects {
    repositories {
        ...
        maven {
            name 'jitpack'
            url "https://jitpack.io"
        }
        ...
    }
}
```

In your application-level build.gradle:

```
implementation 'com.github.SmartMoveSystems:androidfilelogger:3.8'
```

## Setting up to log to file and report crashes

In you application's onCreate() method:

```
    @Override
    public void onCreate()
    {
        super.onCreate()
        ...
        // This will start writing Timber logs to file
        FileLogTree tree = new FileLogTree(getFilesDir(), BuildConfig.DEBUG);
        Timber.plant(tree);
        // The below is optional; use only if you want crashes to be logged
        CrashLogger crashLogger = new CrashLogger(
                activity,
                tree,
                activity.getString(R.string.crash_log_file),
                new ApiConfig(
                  activity.getString(R.string.crash_log_upload_url), // Must be Retrofit-compatible
                  "Logs Uploaded", activity.getString(R.string.type)
                )
        );
        // This will send logs from previous crash to your endpoint if a crash occurred on last run
        crashLogger.prepareCrashLog();
        ...
    }
  ```

## Optional configuration

You can control the size and format of the log files by passing a `LoggerConfig` object to the
`FileLogTree` constructor:

```
LoggerConfig loggerConfig = new LoggerConfig(
    "yyyy-MM-dd_HH-mm-ss-SSS", // fileDateFormat: date format that will appear in log file names
    "yyyy-MM-dd HH:mm:ss.SSS", // logDateFormat: date format that will appear in log entries
    "logs", // logDirName: name of the subdirectory where logs will be saved 
    "log", // logPrefix: prefix for log file names
    ".log", // logExt: log file extension
    1000000, // fileMaxLength: Maximum size of individual log files before rollover, in bytes
    5000000 // totalMaxLength: Maximum size of all log files before old files are deleted
)
FileLogTree tree = new FileLogTree(getFilesDir(), BuildConfig.DEBUG, loggerConfig);
```
  
If no `LoggerConfig` object is provided, the configuration is defaulted to the values in the example
above.
  
## Sending logs manually

```
String subject = "Logs Uploaded";
String body = "This is a message";
String type = "Log type"

LogSender logSender = new LogSender(
        new ApiConfig(activity.getString(R.string.crash_log_upload_url, subject, type),
        logManager // This is your FileLogTree instance
);
logSender.sendLogs(body, new LogSenderCallback() {
    @Override
    public void onSuccess() {
        Timber.i("Succesfully sent logs");
    }

    @Override
    public void onFailure() {
        Timber.e("Failed to send logs");
    }
});
```
