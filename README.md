# मेरो साउन्डबक्स (Mero Soundbox)
Personal eSewa incoming-payment announcer in Nepali. Kotlin, no internet permission, no backend.
**Status: written but NOT compiled or tested by the author.** Treat every test below as yours to run.

## 1. Build the APK on your phone (GitHub Actions)
1. Create a GitHub repo in your phone browser. Add file > Upload files: upload all files from this zip, keeping folders.
   If `.github` is skipped, create `.github/workflows/build.yml` manually and paste its text.
2. Actions tab > Build APK > Run workflow (~5 min). Open the run > Artifacts > download > unzip > install `app-debug.apk`.
   (Allow "install unknown apps" for your browser.) If it fails, copy the red error text and send it to Claude.

## 2. First-time setup (in this order)
1. Open app > button 1 > enable मेरो साउन्डबक्स. Samsung: if greyed out, App info > ⋮ > "Allow restricted settings", retry.
2. Make eSewa send you one notification (a real incoming payment). It appears under CAPTURED NOTIFICATIONS
   (Debug capture is ON by default, and only eSewa-like packages are stored).
   Nothing shown? tick "capture ALL apps", trigger one, read `pkg:`, untick.
3. Verify the package name: Settings > Apps > eSewa > App details (bottom), or Play Store URL `id=...`. Must match `pkg:`.
   Tap "Use package from last captured..." then Save.
4. Fill "Incoming-payment phrase" with a word/phrase that appears in received-money notifications and NOT in sent ones,
   copied from the real notification. Optionally a transaction-ID regex. Save.
5. Paste real notification texts into PARSER TEST. Incoming must ACCEPT; outgoing/failed/promo/OTP must REJECT.
6. Test Nepali voice. If it says "NOT available": install a Nepali-capable engine (e.g. eSpeak NG from F-Droid, or Google
   Speech Services + Nepali voice data), choose it in Android Settings > Text-to-speech, then test again.
   **Listen yourself: if it is unclear, the voice requirement is NOT met.** The app never speaks English instead.
7. Turn Debug capture OFF (or enable Battery-saving mode).

## 3. Samsung background setup
Settings > Apps > मेरो साउन्डबक्स > Battery > Unrestricted. Settings > Battery > Background usage limits: remove the app from
"Sleeping/Deep sleeping apps"; turn off "Put unused apps to sleep" or add it to Never sleeping. Do not swipe-kill from Recents
then Force stop: after Force stop Android will not run it until you open it again. Notification listener re-binds automatically
after reboot once you unlock the phone for the first time; before first unlock it cannot work. Powered-off phone = no announcement.

## 4. Design (battery)
No foreground service, no polling, no wake locks, no network permission. Code runs only when a notification from eSewa (or,
with debug on, an eSewa-like package) arrives. TTS is created per payment and released afterwards. Other packages return immediately.

## 5. Duplicates
With a transaction-ID regex: IDs remembered (last 200), same ID never repeats. Without: same amount + identical text within
60 seconds is ignored; later identical payments are announced.

## 6. Limits
- Parser accepts only if: package matches, no reject word, incoming phrase present, currency-marked amount after it. Else silent.
- Nepali word table (0-99) is written from memory: have a Nepali speaker check it (Settings > test with 21..99 via parser test).
- Max amount 99,99,99,999. Paisa spoken as "पैसा".
- Volume setting scales the speech; phone media volume must also be up.

## 7. Test checklist (do each, note result)
1 Real incoming eSewa -> announces correct amount. 2 Outgoing -> silent. 3 Failed -> silent. 4 Promo -> silent. 5 OTP -> silent.
6 Khalti, 7 bank app -> silent (package differs). 8 Same notification twice -> once. 9 Screen off + locked -> announces.
10 Airplane mode voice test -> works. 11 Reboot, unlock once, receive payment -> announces. 12 Idle battery: charge to 100%,
leave 24h with no payments, check Settings > Battery > usage for this app (should be ~0%).
