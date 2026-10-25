package app.chipos.launcher.data

import java.util.Locale

/**
 * Italian translations. The app's text is written in English in the code; when the language is
 * Italian, [t] swaps it for the Italian version. Text that is not listed here stays in English.
 *
 * Plain sentences are looked up directly. Sentences with a changing part (an app name, a number)
 * are matched by [rules].
 */
object Strings {

    /** The language for dates and the weather lookup. */
    val locale: Locale get() = if (ChipPrefs.italian) Locale.ITALIAN else Locale.ENGLISH

    /** Translates [text] if the language is Italian. Reads ChipPrefs.italian, so Compose redraws on a switch. */
    fun t(text: String): String {
        if (!ChipPrefs.italian) return text
        if (text.length < 2) return text
        italian[text]?.let { return it }
        for (rule in rules) {
            val match = rule.regex.matchEntire(text) ?: continue
            return rule.build(match)
        }
        return text
    }

    private class Rule(val regex: Regex, val build: (MatchResult) -> String)

    private val rules = listOf(
        Rule(Regex("^No apps match \"(.*)\"$")) { "Nessuna app corrisponde a \"${it.groupValues[1]}\"" },
        Rule(Regex("^Looking up \"(.*)\"\\.\\.\\.$")) { "Cerco \"${it.groupValues[1]}\"..." },
        Rule(Regex("^Currently (.*)$")) { "Attuale: ${it.groupValues[1]}" },
        Rule(Regex("^Show \"Good morning\" next to the date$")) { "Mostra \"Buongiorno\" accanto alla data" },
        Rule(Regex("^Closed (\\d+) apps, freed about (\\d+) MB$")) {
            "Chiuse ${it.groupValues[1]} app, liberati circa ${it.groupValues[2]} MB"
        },
        Rule(Regex("^(\\d+) of your own photos found$")) { "${it.groupValues[1]} foto tue trovate" },
        Rule(Regex("^Shows music above the dock\\. (.*)$")) { "Mostra la musica sopra il dock. ${it.groupValues[1]}" },
        Rule(Regex("^Remove an app from the dock first \\(up to (\\d+)\\)$")) {
            "Rimuovi prima un'app dal dock (massimo ${it.groupValues[1]})"
        },
        Rule(Regex("^Pairing with (.*)$")) { "Associazione con ${it.groupValues[1]}" },
        Rule(Regex("^Connected to (.*)$")) { "Connesso a ${it.groupValues[1]}" },
        Rule(Regex("^Connecting to (.*)$")) { "Connessione a ${it.groupValues[1]}" },
        Rule(Regex("^Couldn't join (.*)$")) { "Impossibile collegarsi a ${it.groupValues[1]}" },
        Rule(Regex("^IP address (.*)$")) { "Indirizzo IP ${it.groupValues[1]}" },
        Rule(Regex("^Couldn't open (.*)$")) { "Impossibile aprire ${it.groupValues[1]}" },
        Rule(Regex("^(.*) isn't available on this device$")) {
            "${t(it.groupValues[1])} non è disponibile su questo dispositivo"
        },
        Rule(Regex("^version (.*)$")) { "versione ${it.groupValues[1]}" },
        Rule(Regex("^ready \\((.*)\\)$")) { "pronto (${it.groupValues[1]})" },
        Rule(Regex("^([\\d.,]+ ?[KMGT]?i?B) of ([\\d.,]+ ?[KMGT]?i?B)$")) {
            "${it.groupValues[1]} su ${it.groupValues[2]}"
        },
    )

    private val italian: Map<String, String> = mapOf(
        // Animation speed, wallpaper
        "Fast" to "Veloce",
        "Faster" to "Più veloce",
        "Normal" to "Normale",
        "Both" to "Entrambi",
        "Built-in" to "Integrati",
        "My photos" to "Le mie foto",

        // System shortcuts
        "ADB debugging" to "Debug ADB",
        "Accessibility" to "Accessibilità",
        "Captions, screen reader, magnifier" to "Sottotitoli, screen reader, lente d'ingrandimento",
        "Date & time" to "Data e ora",
        "Developer options" to "Opzioni sviluppatore",
        "Display & resolution" to "Schermo e risoluzione",
        "Language & keyboard" to "Lingua e tastiera",
        "Menu language and keyboards" to "Lingua del menu e tastiere",
        "Screen resolution, HDR and picture options" to "Risoluzione dello schermo, HDR e opzioni immagine",
        "Software update and factory reset" to "Aggiornamento software e ripristino",
        "Time zone and clock" to "Fuso orario e orologio",
        "Updates, restart & reset" to "Aggiornamenti, riavvio e ripristino",

        // Toasts from actions
        "Couldn't open Settings." to "Impossibile aprire le Impostazioni.",

        // Weather
        "Clear" to "Sereno",
        "Drizzle" to "Pioviggine",
        "Fog" to "Nebbia",
        "Mostly clear" to "Prevalentemente sereno",
        "Overcast" to "Coperto",
        "Partly cloudy" to "Parzialmente nuvoloso",
        "Rain" to "Pioggia",
        "Showers" to "Rovesci",
        "Snow showers" to "Rovesci di neve",
        "Snow" to "Neve",
        "Thunderstorm" to "Temporale",
        "Weather" to "Meteo",

        // Wi-Fi
        "Excellent signal" to "Segnale eccellente",
        "Fair signal" to "Segnale discreto",
        "Good signal" to "Segnale buono",
        "Weak signal" to "Segnale debole",
        "Needs TV settings" to "Richiede le impostazioni della TV",
        "Open" to "Aperta",
        "<unknown ssid>" to "<SSID sconosciuto>",
        "  ·  Saved" to "  ·  Salvata",
        "Allow network scanning" to "Consenti la ricerca delle reti",
        "Allow" to "Consenti",
        "Android only lists networks for apps with location permission. ChipOS never reads your location." to
            "Android mostra le reti solo alle app con l'autorizzazione alla posizione. ChipOS non legge mai la tua posizione.",
        "Cancel" to "Annulla",
        "Close" to "Chiudi",
        "Connect" to "Connetti",
        "Connected" to "Connesso",
        "Details" to "Dettagli",
        "Disconnect" to "Disconnetti",
        "Enter the Wi-Fi password." to "Inserisci la password del Wi-Fi.",
        "Forget this network" to "Dimentica questa rete",
        "Forgetting a network means you need its password to join it again." to
            "Dimenticare una rete significa che servirà di nuovo la password per collegarti.",
        "Hidden networks and company or school networks" to "Reti nascoste e reti aziendali o scolastiche",
        "If this stays empty, turn Location on in the TV settings and scan again." to
            "Se resta vuoto, attiva la Posizione nelle impostazioni della TV e cerca di nuovo.",
        "Location" to "Posizione",
        "Looking for networks..." to "Ricerca delle reti...",
        "More" to "Altro",
        "Networks" to "Reti",
        "No networks found" to "Nessuna rete trovata",
        "Refresh the list of networks" to "Aggiorna l'elenco delle reti",
        "Scan again" to "Cerca di nuovo",
        "Searching..." to "Ricerca in corso...",
        "TV Wi-Fi settings" to "Impostazioni Wi-Fi della TV",
        "This network needs the TV's Wi-Fi settings" to "Questa rete richiede le impostazioni Wi-Fi della TV",
        "Wi-Fi passwords have at least 8 characters" to "Le password Wi-Fi hanno almeno 8 caratteri",

        // About
        "About" to "Info",
        "Device" to "Dispositivo",
        "Glass backdrop" to "Sfondo vetro",
        "Hold Select and Play/Pause on the remote for about 5 seconds." to
            "Tieni premuti Select e Play/Pausa sul telecomando per circa 5 secondi.",
        "IP address" to "Indirizzo IP",
        "Memory free" to "Memoria libera",
        "NOT built" to "NON creato",
        "Not connected" to "Non connesso",
        "Now playing" to "In riproduzione",
        "Restart and update" to "Riavvio e aggiornamento",
        "Restart" to "Riavvia",
        "Storage free" to "Spazio libero",
        "System sounds" to "Suoni di sistema",
        "These live in the TV settings." to "Si trovano nelle impostazioni della TV.",
        "Updates" to "Aggiornamenti",
        "not connected" to "non connesso",
        "connected" to "connesso",
        "on" to "attivi",
        "OFF (run android-tv-permissions.bat)" to "DISATTIVATI (esegui android-tv-permissions.bat)",

        // All apps and app options
        "All apps" to "Tutte le app",
        "Hold Select on an app for options" to "Tieni premuto Select su un'app per le opzioni",
        "Search apps" to "Cerca app",
        "Remove from Recent" to "Rimuovi dai Recenti",
        "It comes back the next time you open it" to "Torna la prossima volta che la apri",
        "Add to dock" to "Aggiungi al dock",
        "App info isn't available on this device" to "Info app non disponibili su questo dispositivo",
        "App info" to "Info app",
        "Couldn't start the uninstaller" to "Impossibile avviare la disinstallazione",
        "Dock is full" to "Il dock è pieno",
        "Hide from All apps" to "Nascondi da Tutte le app",
        "Move left in dock" to "Sposta a sinistra nel dock",
        "Move right in dock" to "Sposta a destra nel dock",
        "Remove from dock" to "Rimuovi dal dock",
        "Storage, permissions, force stop" to "Spazio, autorizzazioni, arresto forzato",
        "Undo in Settings > Hidden apps" to "Annulla in Impostazioni > App nascoste",
        "Uninstall" to "Disinstalla",
        "Apps that came with the TV. They can't be uninstalled." to
            "App fornite con la TV. Non possono essere disinstallate.",
        "Apps" to "App",
        "Installed apps" to "App installate",
        "Options" to "Opzioni",
        "Show system apps" to "Mostra app di sistema",
        "Hidden apps" to "App nascoste",
        "Nothing is hidden. Hold Select on an app and choose Hide from All apps." to
            "Nessuna app nascosta. Tieni premuto Select su un'app e scegli Nascondi da Tutte le app.",
        "Select an app to show it in All apps again." to "Seleziona un'app per mostrarla di nuovo in Tutte le app.",
        "Show again" to "Mostra di nuovo",

        // Bluetooth
        "Android needs location permission to search. ChipOS never reads your location." to
            "Android richiede l'autorizzazione alla posizione per cercare. ChipOS non legge mai la tua posizione.",
        "Available devices" to "Dispositivi disponibili",
        "Couldn't start pairing" to "Impossibile avviare l'associazione",
        "Couldn't unpair. Use the TV's Bluetooth settings." to
            "Impossibile dissociare. Usa le impostazioni Bluetooth della TV.",
        "Headphones, speakers, controllers, keyboards" to "Cuffie, altoparlanti, controller, tastiere",
        "No Bluetooth on this device" to "Bluetooth non disponibile su questo dispositivo",
        "Nothing found" to "Nessun risultato",
        "Nothing paired yet" to "Nessun dispositivo associato",
        "Pair" to "Associa",
        "Paired devices" to "Dispositivi associati",
        "Paired" to "Associato",
        "Search for devices" to "Cerca dispositivi",
        "Searching. Put your device in pairing mode." to "Ricerca in corso. Metti il dispositivo in modalità di associazione.",
        "Select to pair" to "Seleziona per associare",
        "Unpair" to "Dissocia",
        "Unpairing removes this device. To use it again, put it in pairing mode and pair it again." to
            "La dissociazione rimuove questo dispositivo. Per riutilizzarlo, mettilo in modalità di associazione e associalo di nuovo.",

        // Weather city dialog
        "Change city" to "Cambia città",
        "City name" to "Nome della città",
        "No city found, or no internet connection" to "Nessuna città trovata, o nessuna connessione a Internet",
        "Search" to "Cerca",
        "Select the box and type a city (for example Milan), then choose Search." to
            "Seleziona la casella e scrivi una città (ad esempio Milano), poi scegli Cerca.",
        "Turn weather off" to "Disattiva il meteo",
        "Weather city" to "Città del meteo",

        // Home screen
        "Settings" to "Impostazioni",
        "RECENT" to "RECENTI",
        "Good morning" to "Buongiorno",
        "Good afternoon" to "Buon pomeriggio",
        "Good evening" to "Buonasera",

        // Settings
        "24-hour clock" to "Orologio 24 ore",
        "Off shows AM and PM" to "Se disattivato mostra AM e PM",
        "All TV settings" to "Tutte le impostazioni della TV",
        "Always show dock names" to "Mostra sempre i nomi nel dock",
        "Ambient screen after" to "Schermata ambient dopo",
        "Animation speed" to "Velocità animazioni",
        "Apps hidden from All apps (hold Select on an app to hide it)" to
            "App nascoste da Tutte le app (tieni premuto Select su un'app per nasconderla)",
        "Closes background apps. Anything playing music is left alone" to
            "Chiude le app in background. Quelle che riproducono musica non vengono toccate",
        "Device, memory, storage, network" to "Dispositivo, memoria, spazio, rete",
        "Dims the home screen and shows a big clock when nothing is pressed" to
            "Oscura la schermata Home e mostra un grande orologio quando non viene premuto nulla",
        "Dock reset to the default apps" to "Dock ripristinato con le app predefinite",
        "Free memory" to "Libera memoria",
        "Freeing memory..." to "Libero la memoria...",
        "Greeting" to "Saluto",
        "Hold Select on any app (here or in All apps) to add, remove or move it" to
            "Tieni premuto Select su un'app (qui o in Tutte le app) per aggiungerla, rimuoverla o spostarla",
        "Home screen" to "Schermata Home",
        "How often the background picture rotates" to "Ogni quanto cambia l'immagine di sfondo",
        "Join networks and see your connection" to "Collegati alle reti e controlla la connessione",
        "Language" to "Lingua",
        "My photos: none yet, run scripts\\push-wallpapers.bat on your PC" to
            "Le mie foto: ancora nessuna, esegui scripts\\push-wallpapers.bat sul PC",
        "Nothing to close" to "Niente da chiudere",
        "Now playing card" to "Scheda In riproduzione",
        "Off shows a name only under the selected app" to "Se disattivato mostra il nome solo sotto l'app selezionata",
        "One-time setup needed: run scripts\\android-tv-permissions.bat on your PC" to
            "Configurazione una tantum necessaria: esegui scripts\\android-tv-permissions.bat sul PC",
        "Open the full system Settings app" to "Apri l'app Impostazioni di sistema completa",
        "Open, inspect and uninstall apps" to "Apri, ispeziona e disinstalla le app",
        "Performance" to "Prestazioni",
        "Recent apps row" to "Riga delle app recenti",
        "Reset" to "Ripristina",
        "Run scripts\\android-tv-permissions.bat first" to "Esegui prima scripts\\android-tv-permissions.bat",
        "Shorter animations make the whole device feel quicker" to "Animazioni più brevi rendono tutto più reattivo",
        "Shown under the date. Uses open-meteo.com; needs internet" to
            "Mostrato sotto la data. Usa open-meteo.com; richiede Internet",
        "Shows the last apps you opened above the dock" to "Mostra le ultime app aperte sopra il dock",
        "Sound" to "Audio",
        "Speed" to "Velocità",
        "Volume and mute" to "Volume e muto",
        "Wallpaper changes every" to "Lo sfondo cambia ogni",
        "Wallpapers" to "Sfondi",

        // Sound
        "Clicks when you move, select and go back. Needs the TV's system sounds turned on" to
            "Clic quando ti muovi, selezioni e torni indietro. Richiede i suoni di sistema della TV attivi",
        "Good to know" to "Da sapere",
        "If your TV or soundbar controls the volume itself, this bar may have no effect. Use your TV remote then." to
            "Se la TV o la soundbar controlla il volume da sola, questa barra potrebbe non avere effetto. Usa il telecomando della TV.",
        "Mute" to "Muto",
        "Navigation sounds" to "Suoni di navigazione",
        "Press left and right to change it" to "Premi sinistra e destra per cambiarlo",
        "Silence media audio" to "Silenzia l'audio multimediale",
        "Volume" to "Volume",
    )
}
