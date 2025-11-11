[app]
title = FichajeApp
package.name = fichajeapp
package.domain = org.tuempresa
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,json

# Servicio
android.services = gpsservice:service/main.py

# Permisos
android.permissions = ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,FOREGROUND_SERVICE,INTERNET

orientation = portrait
icon.filename = %(source.dir)s/icon.png
debug = True

# Librerías requeridas
requirements = python3,kivy==2.3.0,kivymd,gspread,google-auth,plyer,android,cython,pyjnius

# Android SDK
android.api = 34
android.minapi = 28
android.sdk = 34
android.ndk = 25b
android.archs = arm64-v8a,armeabi-v7a
android.entrypoint = org.kivy.android.PythonActivity

version = 1.0
package.version.code = 1
android.private_storage = True
android.allow_backup = True

# Assets
android.add_assets = credentials.json,service/

# Fuera de fullscreen
fullscreen = 0

# Logcat
log_level = 2
logcat_filter = *:S python:D
