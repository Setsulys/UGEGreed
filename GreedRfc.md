1. Introduction 
        Ce document spécifie un protocole de réseau pour la distribution de taches parmi un groupe d'applications.
    Notre protocole permet à des applications liées entre eux avec une structure d'arbre d'appliquer des conjectures et les renvoyer à l'utilisateur
    Chaque application dans l'arbre fonctionne de tel sorte qu'il applique une fonction donnée à une gamme de valeurs données
    -> On récupère un ou plusieurs fichiers jar ou il est contenu une fonction avec laquelle on applique une liste de conjecture fourni dans les fichiers
    Notre protocole nous permet de faire tous les calculs en parallèles pour accélérer la sortie des résultats.

    
2. Aperçu du protocole

    Il est donné des valeurs et une fonction via un jar fournis par l'utilisateur.
    Cette gamme de valeurs sera divisé en sous gamme par le nombre d'application libre et donné à ces applications du protocole.
    Le nombre d'application libre sera retrouvé par un paquet ping que l'application qui a initié la tâche enverra au réseau.
    Chaque application appliquera alors la fonction donnée sur la sous gamme qui lui est donnée et renvoi les résultats dans un fichier texte noté url.
    Quand l'application se déconnecte, elle enverra les données traitées à l'application qui a initié la tâche et les applications enfant se relieront vers l'application père pour récupérer son travail non fini.
    Les applications auront un buffer contenant les valeurs déjà traités, un buffer contenant l’url et les valeurs à traiter et aussi un buffer de stockage pour les tâches d’applications déconnectées. Les buffers de stockage seront traités après que le traitement principal sera terminé. Les différentes données de différentes applications seront séparées par un long égal à 0 dans le buffer de stockage.
    Sa sous-tâche qu'elle était censé traiter sera répartie entre ses connexions.
    Quand la root se déconnectera ce signifiera l'arrêt de chaque application.
    La root n'est déconnectable que si toutes les applications sont en attente.

3. Détails d'implémentations

3.1 Format de communication
    Au début de chaque tâche, l'application qui l'initie envoie à ses applications connectées des paquets ping d'envoie. Chaque application recevant ce paquet et étant libre va émettre à chacune de ses  connexions son paquet ping de réponse.
    Chaque application recevant un paquet ping de réponse le retransmettra à toutes ses autres connexions.
    Si une application reçoit un paquet ping de réponse et est une feuille, elle l'ignore.
    Si cette feuille est libre, alors elle n'enverra uniquement qu'un paquet ping de réponse à sa connexion.
    L'application retransmettra le paquet ping d'envoie à ses connexions dont il ne la pas reçu.
    Les urls seront encodé en ASCII.
    Aussi Chaque application posèdera une table de routage donnant par quel application connexe doit passer l'information pour atteindre la destination.
    On changera la table de routage lors de la déconnexion des applications connexes mais aussi on supprimera l'application des autres tables de routage des applications de tout le réseau




3.2 Transfert de données
    Le partage des données entre les applications et le protocole seront fait en paquets TCP 
    

    3.2.1 Cas d'une Tâche :
    On considère qu'une application par laquelle on fait passer la tâche est l'application source de la tâche.
    Quand une application demande une tâche au réseau, elle va envoyer un paquet ping à chacune de ses connexions et récupèrera quels sont les applications disponibles.
    Elle enverra donc (la gamme de valeurs à traiter / le nombre d'applications disponible) paquets à ses connexions, en précisant quel paquet va vers quelle application directement dans le paquet.
    Quand une application a fini de traiter les données, elle enverra des paquets de données à destination de l'application source qui a demandé la conjecture et sera disponible pour traiter d'autre conjecture ou en démarrer une.
    Quand l'application source a reçu toutes les données, elle produira un fichier texte contenant toute les données traitées, puis restera disponible pour les prochaines conjectures
        Exemple:

            Fonction X
            range [1...20]
            ID Source L  (application actuel)

            Les applications qui sont libre sont A, B,  C
            On enverra ces buffers vers les applications de ce nom
             --------------------------------
            |Opcode | Adresse A | Données... |
             --------------------------------
             --------------------------------
            |Opcode | Adresse B | Données... |
             --------------------------------
             --------------------------------
            |Opcode | Adresse C | Données... |
             --------------------------------

            Forme des données

             -----------------------------------------------------------------------
            |Taille URL (long) |URL(Ascii) |Val range min (int)| Val range max(int) |
             -----------------------------------------------------------------------
    
    3.2.2 Cas d'une application en attente
        Lorsqu'une application vient de se connecter ou qu'elle a fini sa conjecture, elle est en attente.
        Si cette application vient de finir une tâche qui lui a été fournis elle vérifie d'abord si son buffer de stockage ne contiendrait pas des éléments à traiter si oui elle traitera ces éléments sinon elle se mettra en attente d'une nouvelle conjecture.



3.3 Connexion et déconnexion d'une application de l'arbre

    3.3.1 Connexion
        Le réseau acceptera toujours des connexions d'applications, seulement si une conjecture est en cours les nouvelles applications ne participeront pas à cette dernière mais attendrons de recevoir/démarrer une nouvelle conjecture.
        Il est à préciser que chaque application se connectera sur un port unique et une adresse unique, cependant il est possible que sa propre adresse soit partagée par plusieurs applications.

        
    3.3.2 Déconnexion
        La déconnexion d'une application se fera normalement et non brutalement.
        Lors de la déconnexion de l'une des applications, cette dernière signalera sa déconnexion aux applications connexes et à toutes les applications du réseau en envoyant un paquet changement de connexion pour que toutes les applications du réseau changent leurs table de routage et transmettra les données qu'elle n'a pas encore traitée aux applications qui lui sont directement connecté avec des paquets Envoi de données à traiter aux applications en attente.

        Elle séparera les données en divisant la gamme de valeur par le nombre d'applications connexes. Si son buffer de stockage contient des données à traiter d'autre application qui se sont déconnectées, elle divisera et transmettra aussi ces données en plus de l’url du jar.
        Quand une application connexe à celle qui s'est déconnectée, elle reçoit les données de cette dernière, elle les stocke dans son buffer de stockage et les traiteras quand elle sera libre. Si son buffer de stockage est plein elle transmettra, à ses autres voisins.
        Pour ce qui est du stockage s'il y a plusieurs types de tâches dans le buffer de stockage, on les traitera un par un et lorsque l'application aura fini elle sera considérée comme libre.
        Pour ce qui est de la reliaison entre les applications, Si parmi les applications connexes il y a l'application ROOT on lie toutes les applications restantes à celle-ci, sinon on lie toutes les applications à l'application contenant le plus petit des ports. Si il y a deux mêmes ports, on prendra la plus petite adresse ip.

        
        Exemple:
            Application Z en cours de déconnexion à deux applications (A,B) connexe et contient lui meme un buffer contenant des elements d'applications qui se sont déconnecté.
            application initiant la tâche : K
            application initiant une taches ayant une application déja déconnecté : L,M
            FonctionZ
            buffer principal:
             ----------
            | [1...10] |
             ----------
            buffer de stockage d'élément déjà déconnecté:
             ----------------------------------------------------------------------------------------------------------------------------------------------------------------
            |Adresse source L |taille fonction (long) | Fonction1 | [5...10]| 00000000 | Adresse source M |taille fonction (long) | Fonction2 | int range min | int range max |
             ----------------------------------------------------------------------------------------------------------------------------------------------------------------

            Données envoyé à A:
             -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            |OPCODE | Adresse source K | taille fonction (long) | FonctionZ | [1...5] | 00000000 | ID source L | taille fonction (long) | Fonction1 | [1...3] | 00000000 | ID source M | taille fonction (long) | Fonction2 | [5...13] |
             -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            Données envoyé à B:
             ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            |OPCODE | Adresse source K | taille fonction (long) | FonctionZ | [6...10] | 00000000 | ID source L | taille fonction (long) | Fonction1 | [4...5] | 00000000 |ID source M | taille fonction (long) | Fonction2 |[14...20]|
             ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        Ces applications qui receverons les données, traiterons ces conjectures après avoir traité ses propres conjectures.
        Pour cela chaque application a un buffer sur lequel il traite les données et un autre buffer contenant les données de déconnexion.

1.  Structure de l'arbre

                        A 
                    /       \
                  B          C 
                /   \      /   \
               D     E     F     G 
              /\     /\    /\    /
             H  I   J  K  L  M  N  

    On déconnecte C qui a le port (5555)
                        A 
                    /       \
                  B           
                /   \      /   \
               D     E     F    G 
              /\     /\    /\   /
             H  I   J  K  L  M  N  

    A a le port le plus petit parmis F G A
    On lie F G à A
                        A 
                    /     \ \
                  B        \ \ 
                /   \      /  \
               D     E     F    G 
              /\     /\    /\   /
             H  I   J  K  L  M  N  
    

5. Definition des paquets transmis

    (comment definir un type, structure de données, opcode pour voir si c'est une réception ou un aquitement etc...)

    
    Envoi de données à traiter aux applications en attente<br>
     -----------------------------------
    | OPcode | Adresse source | Données |       op code : 0
     -----------------------------------

    Envoi de données de deconnexion à traiter aux applications connexes.
     --------------------------------
    | OPcode | Adresse source | Données |       op code : 1
     --------------------------------
     
    Envoi de données traitées
     -----------------------------------------------------
    | Opcode | Adresse application source | donnee traite |       op code : 2
     -----------------------------------------------------
    
    Demande de connexion
     -------------------------------
    | Opcode | Adresse du demandeur |       op code : 3
     -------------------------------

    acceptation connexion
     --------
    | Opcode |       op code : 4
     --------

    Changement de connexion
     ----------------------------------------
    | Opcode | nouvelle adresse de connexion |       op code : 5
     ----------------------------------------
    
    paquet ping d'envoie
     -------------------------------------
    | Opcode | Adresse Application source |       op code : 6
     -------------------------------------

    paquet ping reponse
     -------------------------------------------------------------------
    | Opcode | Adresse Application source | Adresse Application pinguée |       op code : 7
     -------------------------------------------------------------------

    Opcode sera un int
    Adresse sera un inetSocket
    données sera les differentes données (taille fonctions (long) fonctions ranges ) le tout séparé par un long égale à 0 