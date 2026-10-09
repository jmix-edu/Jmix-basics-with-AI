<!-- BEGIN jmix-agent-toolkit -->
## Jmix

This is a Jmix 3 application. Before writing or changing ANY file in it, read
the `jmix` skill and follow it. It maps the task to artifacts, routes each
artifact to the skill that governs it, and names the checks that close a task.
Your Jmix/Vaadin priors are not reliable here; the skills are.

Managed by the Jmix Agent Toolkit. Content between these markers is replaced on
re-install — put your own instructions outside them.
<!-- END jmix-agent-toolkit -->

## Spec-Driven Workflow

- Порядок работы: спека → ревью спеки пользователем → реализация → Review → Verify.
  Спеку пишешь по промпту пользователя и ждёшь его одобрения.
- Код пишешь только после явной команды «Реализуй ...». Фраза «... Примени к спеке,
  код пока не пиши» значит: правишь только спеку.
- Путь спеки: `specs/<NN_module>/<NN>-<slug>.spec.adoc`. Если путь не задан, модуль
  выводи из темы.
- Модули спек = уроки курса: `01_foundations`, `02_security`, `03_files`,
  `04_deployment`, `05_addons`.
- Никаких глобальных команд очистки (`docker system/volume/image prune`, `rm -rf` вне
  проекта). Удаляешь только то, что создал сам, и по имени.
- Мелкие разовые правки (дефолты формы, бин-сидер) делаешь сразу, без спеки, и
  говоришь, что сделал.
- Стандартные CRUD list/detail экраны и Liquibase changelog генерирует Jmix Studio,
  не ты. Если не уверен в стандартном артефакте Jmix, попроси у пользователя образец
  из Studio и повтори его.
- Приложение поддерживает две локали, `ru_RU` (по умолчанию) и `en`
  (`jmix.core.available-locales=ru_RU,en`). Каждый новый ключ пишешь в оба бандла:
  по-русски в `messages_ru_RU.properties`, по-английски в `messages_en.properties`.
  Наборы ключей в бандлах совпадают.
- Гейты: Gate 1 и Gate 2 — как требует тулкит. Gate 3: браузер сам не обходишь, в
  конце даёшь короткий чек-лист, что пользователю проверить в браузере.
- Тесты пишешь только там, где поведение трудно проверить руками (безопасность,
  бизнес-правила).
- Не коммить: пользователь коммитит сам после каждого шага.
