# TinyRadiusServerMFA
A simple server based on 'TinyRadius: Java Radius library' with challenge

# Build
```
mvn package
```

# Config

The server loads settings from file **config.properties**

```
radius.secret=yourSharedCode

debug=1

user.1.name=John
user.1.phone=+1555555555
user.1.password=youPassword123

user.2.name=Augsut
user.2.phone=+1444444444
user.2.password=youPassword456

user.3.name=Lars
user.3.phone=+1333333333
user.3.password=youPassword789

twilio.sid=TwilioSID
twilio.token=TwilioToken
twilio.from=TwilioFromPhone
```

# Run
```
java -jar TinyRadiusServerMFA-1.1.0.jar
```

# Sucess run
```
Apr 29, 2024 4:37:43 PM org.tinyradius.util.RadiusServer run
INFO: starting RadiusAuthListener on port 1812
```

# Testing

For Windows I recommend using NTRadPing | https://ntradping.apponic.com/
