[app]
# Nombre de tu aplicación
title = FichajeApp

# Nombre del paquete (debe ser único en todo Android)
package.name = fichajeapp
package.domain = org.tuempresa  # puedes cambiarlo por algo como org.miempresa.fichaje

# Archivo principal
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,json
main.py = main.py

# Incluir la carpeta del servicio
android.services = gpsservice:service/main.py

# Permisos necesarios para GPS y servicio en segundo plano
android.permissions = ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,FOREGROUND_SERVICE,INTERNET

# Orientación de la app
orientation = portrait

# Icono (opcional)
icon.filename = %(source.dir)s/icon.png

# Modo depuración
debug = True

# Librerías requeridas
requirements = python3,kivy==2.3.0,kivymd,gspread,google-auth,plyer,android,jnius

# Versión mínima de Android (API)
android.api = 34
android.minapi = 28
android.sdk = 34
android.ndk = 25b

# Modo de compilación
android.archs = arm64-v8a,armeabi-v7a
android.entrypoint = org.kivy.android.PythonActivity

# Versión de la app
version = 1.0
package.version.code = 1

# Carpetas de datos adicionales (donde se guardarán registros)
android.private_storage = True
android.allow_backup = True

# Librerías externas que puedan necesitar acceso nativo
p4a.local_recipes =

# --- Configuración de entorno ---
log_level = 2
fullscreen = 0

# --- Para compilar correctamente en Android ---
# Asegúrate de tener tu archivo de credenciales y JSON dentro del directorio principal
android.add_assets = credentials.json,service/

# --- Librerías que requieren acceso a Android ---
requirements.source.kivymd = https://github.com/kivymd/KivyMD/archive/master.zip

# --- Configuración de logcat (para depuración) ---
logcat_filter = *:S python:D

# --- Opciones de compilación adicionales ---
# (Estas evitan errores por dependencias recientes de google-auth)
requirements.source.google-auth = https://github.com/googleapis/google-auth-library-python.git

[buildozer]
