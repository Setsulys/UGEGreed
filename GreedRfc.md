```
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
             -------------------------------------------------------------------------
            |Taille URL (long) | URL (Ascii) |Val range min (int)| Val range max(int) |
             -------------------------------------------------------------------------
    
    3.2.2 Cas d'une application en attente
        Lorsqu'une application vient de se connecter ou qu'elle a fini sa conjecture, elle est en attente.
        Si cette application vient de finir une tâche qui lui a été fournis elle vérifie d'abord si son buffer de stockage ne contiendrait pas des éléments à traiter si oui elle traitera ces éléments sinon elle se mettra en attente d'une nouvelle conjecture.

    3.2.3 Table de routage

        La table de routage sera considéré comme un dictionnaire ou toutes les routes vers toutes les applications seront définie a l'aide des applications connexes à l'application
        Pour créer sa table de routage une application va regarder d'abord ses voisin (pere et fils (normalement que son pere vu que l'application viens de se connecter)) 
        ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????


        Exemple de table de routage

                        A 
                    /       \
                  B          C 
                /   \      /   \
               D     E     F     G 
              /\     /\
             H  I   J  K                  

               On admet ici que l'on est B
               notre table de routage sera :

               A -> A
               C -> A
               D -> D
               E -> E
               F -> A
               G -> A
               H -> D
               I -> D
               J -> E
               K -> E

3.3 Connexion et déconnexion d'une application de l'arbre

    3.3.1 Connexion
        Le réseau acceptera toujours des connexions d'applications, seulement si une conjecture est en cours les nouvelles applications ne participeront pas à cette dernière mais attendrons de recevoir/démarrer une nouvelle conjecture.
        Il est à préciser que chaque application se connectera sur un port unique et une adresse unique, cependant il est possible que sa propre adresse soit partagée par plusieurs applications.

        
    3.3.2 Déconnexion
        La déconnexion d'une application se fera normalement et non brutalement.
        Lors de la déconnexion de l'une des applications, cette dernière signalera sa déconnexion aux applications connexes et à toutes les applications du réseau en envoyant un paquet changement de connexion pour que toutes les applications du réseau changent leurs table de routage et aussi elle transmettra les données qu'elle n'a pas encore traitée aux applications qui lui sont directement connecté avec des paquets Envoi de données à traiter aux applications en attente.

        Elle séparera les données en divisant la gamme de valeur par le nombre d'applications connexes. Si son buffer de stockage contient des données à traiter d'autre application qui se sont déconnectées, elle divisera et transmettra aussi ces données en plus de l’url du jar.
        Quand une application connexe à celle qui s'est déconnectée, elle reçoit les données de cette dernière, elle les stocke dans son buffer de stockage et les traiteras quand elle sera libre. 
        Si son buffer de stockage est plein elle transmettra, à ses fils.
        Pour ce qui est du stockage s'il y a plusieurs types de tâches dans le buffer de stockage, on les traitera un par un et lorsque l'application aura fini elle sera considérée comme libre.
        Pour ce qui est de la reliaison entre les applications, on relie toutes les applications fils de l'application se déconnectant a son père.

        
        Exemple:
            Application Z en cours de déconnexion à deux applications (A,B) connexe et contient lui-même un buffer contenant des éléments d'applications qui se sont déconnecté.
            A etant le père de Z et B le fils de Z
            Application initiant la tâche : K
            Application initiant une tache ayant une application déjà déconnecté : L,M
            URL de l'application sera l'URL Z

            Buffer d'element déja traités
             -----------------------------------------------------------------------------
            | Adresse Source K |Taille valeur (long) | 1 | ... | Taille valeur (long) | 3 |
             -----------------------------------------------------------------------------

            Buffer Contenant les element non traités
             -------------------------------------------------
            | Adresse Source K | Taille URL Z (long) | 4 | 10 |
             -------------------------------------------------

            Buffer de stockage d'element d'application déja déconnecté : 
             ----------------------------------------------------------------------------------------------------------------
            | Adresse Source L | Taille URL L (long) | 5 | 10 | 0000 0000 | Adresse Source M | Taille URL M | URL M | 5 | 20 |
             ----------------------------------------------------------------------------------------------------------------

             ______________________________________________________

            Lors de la déconnexion

            Données envoyés a A
             -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            | OPCODE | Adresse Source K | Taille URL Z (long) | 4 | 7 | 0000 0000 | Adresse Source L | Taille URL L (long) | 5 | 7 | 0000 0000 | Adresse Source M | Taille URL M | URL M | 5 | 12 |
             -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

            Données envoyés a B
             ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
            | OPCODE | Adresse Source K | Taille URL Z (long) | 8 | 10 | 0000 0000 | Adresse Source L | Taille URL L (long) | 8 | 10 | 0000 0000 | Adresse Source M | Taille URL M | URL M | 12 | 20 |
             ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


            ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

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

    On déconnecte C 
                        A 
                    /       \
                  B           
                /   \      /   \
               D     E     F    G 
              /\     /\    /\   /
             H  I   J  K  L  M  N  

    On lie au pere de C qui est donc A
                        A 
                    /     \ \
                  B        \ \ 
                /   \      /  \
               D     E     F    G 
              /\     /\    /\   /
             H  I   J  K  L  M  N  
    

5. Definition des paquets transmis

    (comment definir un type, structure de données, opcode pour voir si c'est une réception ou un aquitement etc...)

    Definition des données
     -------------------------------------------------------------------------
    |Taille URL (long) | URL (Ascii) |Val range min (int)| Val range max(int) |
     -------------------------------------------------------------------------

    Envoi de données à traiter aux applications en attente<br>
     -----------------------------------
    | OPcode | Adresse source | Données |                                           Op code : 0
     -----------------------------------

    Envoi de données de deconnexion à traiter aux applications connexes.
     -----------------------------------
    | OPcode | Adresse source | Données |                                           Op code : 1
     -----------------------------------
     
    Envoi de données traitées
     -----------------------------------------------------
    | Opcode | Adresse application source | donnee traite |                         Op code : 2
     -----------------------------------------------------
    
    Demande de connexion
     -------------------------------
    | Opcode | Adresse du demandeur |                                               Op code : 3
     -------------------------------

    acceptation connexion
     --------
    | Opcode |                                                                      Op code : 4
     --------

    Changement de connexion lors de la déconnexion / Changement des tables de routages
     --------------------------------------------------------
    | opcode | Adresse application | Adresse application pere|                      Op code : 5
     --------------------------------------------------------

    Ping de retour du changement des tables de routages avant la déconnexion
     ---------------
    | Opcode | Byte |                                                               Op code : 6
     ---------------
    On a 1 ou 0 selon si l'operation s'est bien déroulé

    Trame ping d'envoie
     -------------------------------------
    | Opcode | Adresse Application source |                                         Op code : 7 
     -------------------------------------

    Trame ping reponse
     -------------------------------------------------------------------
    | Opcode | Adresse Application source | Adresse Application pinguée |           Op code : 8
     -------------------------------------------------------------------

    Trame de transfert des données
     --------------------------------------------------------------------------
    | Opcode | Données A | 0000 0000 | Données B | 0000 0000 | Données C | ... |    Op code : 9
     --------------------------------------------------------------------------

    Opcode sera un int
    Adresse sera un inetSocket
    données sera les differentes données (taille fonctions (long) fonctions ranges ) le tout séparé par un long égale à 0 

```