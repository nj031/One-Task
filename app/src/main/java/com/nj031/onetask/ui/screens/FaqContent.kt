package com.nj031.onetask.ui.screens

/**
 * Static Help & FAQ content. Kept as plain Kotlin data (not string resources) since this is a
 * large, self-contained block of informational copy specific to this one screen, not reusable UI
 * chrome - splitting ~40 Q&A pairs across strings.xml would add a lot of noise for no benefit
 * here. Content matches the product-specified questions/answers; a small number of answers were
 * corrected to match One Task's actual current behavior (see inline notes) rather than aspirational
 * or marketing copy (e.g. features listed as "Pro-only" on the Upgrade to Pro screen that aren't
 * actually gated yet).
 */
data class FaqQuestion(val question: String, val answer: String)
data class FaqCategory(val id: String, val title: String, val questions: List<FaqQuestion>)

val faqCategories: List<FaqCategory> = listOf(
    FaqCategory(
        id = "account_login",
        title = "Account & Login",
        questions = listOf(
            FaqQuestion(
                "Can I use One Task without an account?",
                // One Task requires sign-in everywhere (see NavGraph's startDestination) - there
                // is no guest/skip mode, so this is corrected from the spec's "Yes" wording.
                "No. You need to sign in to a One Task account to use the app."
            ),
            FaqQuestion(
                "How do I log out?",
                "Open the hamburger menu and tap Log Out. Confirm the logout when prompted. You'll be taken to the Login screen."
            ),
            FaqQuestion(
                "What happens to my data when I log out?",
                "Logging out does not delete your account or your data. Your data remains associated with your One Task account. When you sign in again with the same account, your data will be available again."
            ),
            FaqQuestion(
                "Can I delete my account and all my data?",
                // Corrected menu path: the Data & Privacy row is labeled "Delete Account", not
                // "Delete Your Account" (that phrase is the first confirmation dialog's title).
                "Yes. Go to Settings → Data & Privacy → Delete Account. You'll be asked to confirm twice. Once confirmed, your account and associated data will be permanently deleted and cannot be recovered."
            )
        )
    ),
    FaqCategory(
        id = "tasks",
        title = "Tasks",
        questions = listOf(
            FaqQuestion(
                "How do I create a task?",
                "Tap the + Add Task button, enter a task name, choose the options you need, and tap Create Task."
            ),
            FaqQuestion(
                "How do I edit or delete a task?",
                "Tap the task to open its actions. Choose Edit to change the task or Delete to remove it. Deletion requires confirmation."
            ),
            FaqQuestion(
                "How do subtasks work?",
                "You can add unlimited subtasks to a task. Complete each subtask as you work. When all subtasks are completed, the task can be completed as well."
            ),
            FaqQuestion(
                "How do recurring tasks work?",
                "When creating or editing a task, choose Repeat and select a recurring option such as Daily, Weekly, or Monthly."
            ),
            FaqQuestion(
                "What happens when I postpone a task?",
                // Precision: this only applies to a task that hasn't been started yet (an
                // in-progress/paused overdue task isn't auto-postponed).
                "If Pending Task is enabled, an incomplete task that hasn't been started yet is carried forward to the next day until it is completed."
            ),
            FaqQuestion(
                "What are task tags?",
                "Tags help you organize tasks by category. One Task includes basic tags such as Personal, Work, Study, and Health."
            ),
            FaqQuestion(
                "Can I create my own custom task tags?",
                // Corrected: custom tags aren't actually gated behind Pro - any signed-in user
                // can create one today (the Upgrade to Pro screen's comparison table is purely
                // informational and doesn't restrict anything yet).
                "Yes. You can create your own custom tags in addition to the basic ones."
            ),
            FaqQuestion(
                "How does task status work?",
                "Tasks have three statuses: Not Started, In Progress, and Done. Tasks without a timer use Not Started and Done, while timed tasks can also be In Progress."
            ),
            FaqQuestion(
                "Can I change the date of a task?",
                "Yes. Edit the task and choose a different date. You can select Today, Tomorrow, or a custom date."
            )
        )
    ),
    FaqCategory(
        id = "focus_timer",
        title = "Focus Mode / Timer",
        questions = listOf(
            FaqQuestion(
                "How do I start a Focus Timer?",
                "Open a timed task and select Start. The Focus Timer will open and begin automatically."
            ),
            FaqQuestion(
                "What happens when I pause the timer?",
                "The timer pauses and keeps the exact remaining time. Your task stays In Progress. You can resume the session later."
            ),
            FaqQuestion(
                "What happens when I reset the timer?",
                "Reset returns the timer to its original duration and stops the session. The task returns to Not Started, and the timer does not start automatically."
            ),
            FaqQuestion(
                "Can I take a break during a focus session?",
                "Yes. Tap Break and confirm. Your timer will pause and keep the remaining time. Focus Mode will close and you'll return to your task list."
            ),
            FaqQuestion(
                "What happens when the timer finishes?",
                "The focus session ends and the timer stops. If all subtasks are completed, the task is automatically marked as Done. Otherwise, you can choose to mark the task Done or continue working on it."
            ),
            FaqQuestion(
                "Can I continue a task after the timer finishes?",
                "Yes. If the task is not complete, choose Continue Task to keep working on it."
            ),
            FaqQuestion(
                "What happens if I leave Focus Mode?",
                "One Task asks you to confirm before leaving. If you choose Leave Focus, the timer pauses and keeps the remaining time, and you return to your task list. You can continue the task later."
            ),
            FaqQuestion(
                "Can I use tasks without a timer?",
                "Yes. A timer is optional. You can create a task with No Timer and complete it normally."
            )
        )
    ),
    FaqCategory(
        id = "notifications",
        title = "Notifications",
        questions = listOf(
            FaqQuestion(
                "How do One Task notifications work?",
                "One Task uses notifications to keep you informed about active Focus sessions and other supported app events."
            ),
            FaqQuestion(
                "Will my Focus Timer keep running when I leave the app?",
                "Yes. Your Focus Timer continues running when you leave the app or lock your device."
            ),
            FaqQuestion(
                "Will I get a notification when my Focus session ends?",
                "Yes. One Task sends a notification when your Focus session is complete."
            ),
            FaqQuestion(
                "Why do I need to allow notifications?",
                "Notification permission allows One Task to notify you about important events, such as completed Focus sessions."
            ),
            FaqQuestion(
                "Can I turn notifications off?",
                "Yes. Open the hamburger menu, tap General, then Notifications, to turn Focus Session Notifications and Focus Session Complete on or off individually. You can also control One Task's overall notification permission from your device's notification settings."
            )
        )
    ),
    FaqCategory(
        id = "data_sync",
        title = "Data & Sync",
        questions = listOf(
            FaqQuestion(
                "How does Local Backup work?",
                "Local Backup lets you save your One Task data on your device. You can use the backup to restore your data later."
            ),
            FaqQuestion(
                "What is Cloud Sync?",
                // Corrected: Cloud Sync isn't actually Pro-gated - it already runs
                // unconditionally for every signed-in user.
                "Cloud Sync automatically keeps your One Task data backed up to your account whenever you're signed in."
            ),
            FaqQuestion(
                "Can I use One Task on multiple devices?",
                // Corrected: multi-device sign-in isn't restricted in any way today.
                "Yes. You can sign in to your One Task account on multiple devices."
            ),
            FaqQuestion(
                "What happens if I delete my data?",
                "Deleted data is permanently removed and cannot be recovered. Delete All Data removes your stored One Task data while keeping your account active."
            ),
            FaqQuestion(
                "Can I export my One Task data?",
                "Yes. Use Create Local Backup in Data & Privacy and choose where you want to save your exported data."
            ),
            FaqQuestion(
                "Can I restore my data from a backup?",
                "Yes. Use Restore from Local Backup and select a compatible One Task backup file from your device."
            ),
            FaqQuestion(
                "Is my data safe?",
                "One Task uses your account and its data-storage systems to keep your data associated with your account. Keep your login details secure and only restore backup files you trust."
            )
        )
    ),
    FaqCategory(
        id = "general",
        title = "General",
        // General Settings now exists (hamburger menu > General) with all 7 rows fully
        // functional, including Appearance (Display Mode, Theme color, and a Wallpaper section
        // that's still a placeholder pending real wallpaper assets).
        questions = listOf(
            FaqQuestion(
                "How do I change the app appearance?",
                "Open the hamburger menu, tap General, then Appearance. You can switch between System Default, Light, and Dark display modes, and choose a color theme (Blue, Green, Teal, Amber, or Pink) - both apply instantly across the app and are remembered the next time you open One Task."
            ),
            FaqQuestion(
                "How do I change the start screen?",
                "Open the hamburger menu, tap General, then Start Screen, and choose Tasks or Journal. This controls which section opens when you launch One Task."
            ),
            FaqQuestion(
                "Can I set a default timer for new tasks?",
                "Yes. Open the hamburger menu, tap General, then Default Task Settings, and choose a Default Timer (No Timer, 25, 45, 60 minutes, or Custom). New tasks start with this timer already selected - you can still change it per task, and existing tasks are never affected."
            ),
            FaqQuestion(
                "Can I change my default task settings?",
                "Yes. Open the hamburger menu, tap General, then Default Task Settings, to set a default Timer, Tag, and whether Pending Task is on by default for new tasks. Changing these never affects tasks you've already created."
            ),
            FaqQuestion(
                "How do I manage notifications?",
                "Open the hamburger menu, tap General, then Notifications, to turn Focus Session Notifications and Focus Session Complete on or off. Task Reminders is a planned Pro feature and isn't available yet. You can also manage notification permissions for One Task from your device's system settings."
            ),
            FaqQuestion(
                "Can I change which day my week starts on?",
                "Yes. Open the hamburger menu, tap General, then Week Starts On, and choose any day of the week. This only changes how days are ordered in the calendar - it never moves or changes your task dates."
            ),
            FaqQuestion(
                "Can I switch between 12-hour and 24-hour time?",
                "Yes. Open the hamburger menu, tap General, then Time Format, and choose System Default, 12-hour, or 24-hour. This applies wherever One Task shows a time, such as Journal note timestamps."
            ),
            FaqQuestion(
                "What is haptic feedback?",
                "Haptic feedback is a subtle vibration for meaningful actions, like completing a task, starting or pausing your timer, or picking a date. You can turn it on or off from the hamburger menu under General → Haptic Feedback - it's on by default."
            )
        )
    )
)
