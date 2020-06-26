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
implementation 'com.github.SmartMoveSystems:androidfilelogger:5.2'
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
    val tree = FileLogTree(filesDir, BuildConfig.DEBUG)
    Timber.plant(tree)
    logManager = tree
    apiConfig =  ApiConfig(
        getString(R.string.crash_log_upload_url), // Must be Retrofit-compatible
        mapOf("paramOne" to "valueOne") // Optional string parameters to be sent with every request
    )
    // The below is optional; use only if you want crashes to be logged
    val crashLogger = CrashLogger(
        this,
        tree,
        getString(R.string.crash_log_file), // name of crash log file (i.e. crash.txt)
        apiConfig
    )
    // This will send logs from previous crash to your endpoint if a crash occurred on last run
    crashLogger.prepareCrashLog()
    ...
}
```

## Optional configuration

You can control the size and format of the log files by passing a `LoggerConfig` object to the
`FileLogTree` constructor:

```
val loggerConfig = LoggerConfig(
    "yyyy-MM-dd_HH-mm-ss-SSS", // fileDateFormat: date format that will appear in log file names
    "yyyy-MM-dd HH:mm:ss.SSS", // logDateFormat: date format that will appear in log entries
    "logs", // logDirName: name of the subdirectory where logs will be saved 
    "log", // logPrefix: prefix for log file names
    ".log", // logExt: log file extension
    1000000, // fileMaxLength: Maximum size of individual log files before rollover, in bytes
    5000000 // totalMaxLength: Maximum size of all log files before old files are deleted
)
val tree = new FileLogTree(getFilesDir(), BuildConfig.DEBUG, loggerConfig);
```
  
If no `LoggerConfig` object is provided, the configuration is defaulted to the values in the example
above.
  
## Sending logs manually

```
val sender = LogSender(
    apiConfig,
    logManager // This is your FileLogTree instance
)
sender.sendLogs(object : LogSenderCallback {
    override fun onSuccess() {
        Timber.i("Congrats, your logs were sent")
    }

    override fun onFailure() {
        Timber.e("Oops, looks like something went wrong")
    }
})

sender.sendLogs(object : LogSenderCallback {
    override fun onSuccess() {
        Timber.i("Congrats, your logs were sent")
    }

    override fun onFailure() {
        Timber.e("Oops, looks like something went wrong")
    }
}, LogSender.ALL_FILES, mapOf("extraPart" to "extraValue"))
```
