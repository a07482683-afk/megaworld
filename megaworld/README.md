# Mega World 0.3.0 (Fabric 1.20.1)

Это исходники мода. Чтобы получить .jar, выбери ОДИН способ:

## Способ 1. Windows, двойной клик
1. Установи JDK 17: https://adoptium.net
2. Запусти build.bat (сам скачает Gradle и соберёт мод).
3. Готовый файл: build\libs\megaworld-0.3.0.jar  (не тот, что с -sources).

## Способ 2. Без установки Java (облако GitHub)
1. Создай репозиторий на github.com и залей туда все файлы этой папки.
2. Вкладка Actions -> build -> Run workflow.
3. Когда закончится, скачай артефакт megaworld-jar и достань из него jar.

## Способ 3. Linux / macOS
./build.sh

## Установка
1. Fabric Loader для Minecraft 1.20.1 (https://fabricmc.net).
2. Fabric API для 1.20.1 в папку mods.
3. megaworld-0.3.0.jar тоже в папку mods.

Если сборка выдаст ошибку, пришли текст ошибки.
