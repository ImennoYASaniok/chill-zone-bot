# Очистка кэша (удалить вручную, если занято)
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle" -ErrorAction SilentlyContinue

# Компиляция core и data модулей
.\gradlew.bat :core:compileKotlin :data:compileKotlin --stacktrace

# Установка app
.\gradlew.bat :app:installDist --no-daemon --stacktrace
