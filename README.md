# androidfilelogger
SmartMove Android File Logger

```
implementation 'com.github.SmartMoveSystems:androidfilelogger:3.7'
```

## Setting up to log and report crashes

In you application's onCreate() method:

```
    @Override
    public void onCreate()
    {
        super.onCreate()
        ...
        FileLogTree tree = new FileLogTree(getFilesDir(), BuildConfig.DEBUG);
        Timber.plant(tree);
        crashLogger = new CrashLogger(
                activity,
                tree,
                activity.getString(R.string.crash_log_file),
                new ApiConfig(
                  activity.getString(R.string.crash_log_upload_url), // Must be Retrofit-compatible
                  "Logs Uploaded", activity.getString(R.string.type)
                )
        );
        crashLogger.prepareCrashLog();
        ...
    }
  ```

  
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
