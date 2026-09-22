# R8 rules for the demo shell.
#
# Empty on purpose: every library used so far (Compose, Hilt, DataStore, Navigation) ships its
# own consumer rules, and the release build is verified to work without additions. Rules are
# added here only when a real release build proves one is missing — never pre-emptively.
#
# Keep line numbers meaningful in crash reports once Crashlytics is wired up (Phase 3):
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
