# TinyRadiusServerMFA
A simple server based on tinyradius with challenge

# Build
```
mvn package
```

# Config

The server loads settings from file **config.properties**

```
radius.secret=yourSharedCode

user.name=John123
user.phone=+1555555555
user.password=youPassword123

twilio.sid = TwilioSID
twilio.token = TwilioToken
twilio.from = TwilioFromPhone
```

# Run
```
java -jar TinyRadiusServerMFA-1.0.0.jar
```

# Sucess run
```
Apr 29, 2024 4:37:43 PM org.tinyradius.util.RadiusServer run
INFO: starting RadiusAcctListener on port 1813
Apr 29, 2024 4:37:43 PM org.tinyradius.util.RadiusServer run
INFO: starting RadiusAuthListener on port 1812
```

# Testing

For Windows I recommend using NTRadPing | https://ntradping.apponic.com/

