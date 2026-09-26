# Keep 1.3.0 settings keys and values

Settings are stored under the 1.3.0 keys (`mode`, `group_size`, `palette_name`, `decision_timeout_sec`) with the 1.3.0 Mode strings `"ChooseOne"`, `"groups"`, `"DefineOrder"`, even though they no longer match the code's names. Upgrading users keep their settings with no migration; a migration was rejected as risk with no user benefit. Do not "tidy" these strings.
