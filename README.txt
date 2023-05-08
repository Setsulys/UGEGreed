Pour pouvoir lancer l'application Aller dans le repertoire UGEGreed/bin/

lancer les commandes:
- pour la Racine:
 java fr.uge.greed.Main <Adresse> <port> 
- pour les autres nodes :
 java fr.uge.greed.Main <Adresse> <port> <Adresse Server> <Port Server>
	DISCONNECT deconnecte l'application
	START <URL/JAR> <Nom de classe> <valeur de début> <valeur de fin> <fichier de sortie>
Pour pouvoir lancer le fichier qui prendra en compte les jar (comme on a pas fini)
 java fr.uge.ugegreed.JarTreatement <URL/JAR> <Nom de classe> <valeur de début> <valeur de fin> <fichier de sortie>

la classe determinera automatiquement si c'est un fichier ou un url
