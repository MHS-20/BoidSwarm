## Model & Boids ##
Ogni attore deve avere il proprio model?
Nel messaggio mando solo i nearby boids, oppure ogni volta una copia dei boid?
Perché se invio solo i nearby boids il calcolo lo deve fare tutto l'attore principale. 
Mandando una copia dei boid si occupa più memoria ma almeno non deve fare lui il calcolo. 
In questo modo ogni attore avrebbe il proprio model. 

Il manager riceve il model creato dal main, e poi ogni volta che viene resettato si crea un nuovo model.

Il problema è che gli oggetti vengono passati per riferimento, quindi ci sono corse critiche.
Se tieni separate le due fasi di Calculate e Update velocity non ci sono corse critiche.  
UpdatePosition non c'è bisogno di fare un messaggio separato, si mandano il messaggio da soli. 

Si dovrebbero clonare gli oggetti? Dovrei assumere che sono su nodi diversi?
Se non possono comunicare tramite memoria condivisa devono per forza farsi delle copie. 
Altrimenti ognuno vede i side-effect degli altri, e non va bene.

Il problema è che se passo il model come oggetto di un messaggio viene passato per riferimento.
Perché per fare che il model non sia condiviso ognuno dovrebbe creare il proprio oggetto model.
Quindi servono dei messaggi all'inizio per far creare il model a tutti quanti.
Oppure passo il model e tutti lo clonano?

Anche quando passo la lista di boid, dovrei farne la deep copy.
Oppure puoi lasciarla condivisa ma non farli comunicare tramite la memoria condivisa.

Ognuno riceve una copia del model all'inizio.
I boid rimangono condivisi ma nessuno vede le modifiche degli altri. 
Perché prima tutti leggono e poi tutti scrivono.
Al giro successivo tutti ricevono la nuova lista.

Ma in realtà dipende tutto da chi crea i boid.
Se ogni attore crea il proprio boid, è un gemello indipendente.
Basta ricevere la lista di boid, e fare tutto l'aggiornamento in un colpo solo e ritornare il boid aggiornato. 
Il boid viene mandato indietro aggiornato svuotando e riempiendo di nuovo la lista. 

### Inefficiente 
È troppo inefficiente fare che ogni attore ha i suoi oggetti, fare un numero di attori pari al numero di core?
Se akka ha un pool di thread, non dovrebbe cambiare così tanto, dopo dovrei modificare il protocollo.
Dovrei usare delle classi immutabili, in java devo farlo a mano (in realtà va già bene così)

Il problema maggiore è fare le copie degli oggetti e scambiarsi tanti messaggi. 
E' pesante mandare in modo seriale lo stesso messaggio a tutti, o raccogliere tutti i risultati.
Potrei sfruttare dei sotto-manager per parallelizzare l'invio di messaggi? 
Non posso crearmi dei thread miei esterni ad akka perché rompo l'astrazione.

DOMANDA: basta fare new Boid nel BoidActor, oppure devo copiare anche Pos & Vel?

### TO DO & DeepCopies
- Fare messaggi con oggetti immutabili
1. Ognuno deve avere una copia del model (fatto)
2. La lista di boid è condivisa ed ognuno lavora su un boid copia
3. Il manager si tiene il model ed una lista in cui raccogliere i boid 
4. Decidere se le copie vengono fatte da chi manda il messaggio o da chi lo riceve 
5. La generazione di nuovi boid può essere fatta dalla view così si vede subito (però così salti il controller)
6. Riutilizzare gli stessi attori senza crearli di nuovo (non puoi perché cambia il numero di boid)
7. Fare il calcolo del framerate con uno scheduler (ho provato e non funziona)
8. C'è un modo più efficiente per creare gli attori invece che fare delle deep copy ogni volta? 

Ognuno dovrebbe lavorare sulla propria copia
Per messaggio si dovrebbero sempre passare delle copie.

Ricevono la lista di boid che è condivisa e rimane condivisa,
poi ognuno modifica il proprio boid, che è una copia fatta all'inizio. 
Dopo viene creata una nuova lista, che deve essere mandata anche alla view.
Lo sfarfallamento era probabilmente dovuto al fatto che facevi clear della lista.

Invece il model? Ognuno fa set boids sul model quando riceve la lista.
Anche quello viene ricevuto come parametro per accedere ai weigths.
La cosa più giusta sarebbe che ogni attore si crea il proprio oggetto model. 
All'inizio vengono inviati tutti i parametri, o vengono messi a un valore di default.
Il model viene creato dal main e passato al manager, che lo usa per trasmettere i valori.
A ogni giro ricevono la lista che anche se condivisa va bene perché ognuno aggiorna il twin.

Però manager e view hanno entrambi il model ed hanno una dipendenza circolare.
Ognuno dovrebbe avere la sua copia altrimenti il manager non riesce ad aggiornare la lista.
In realtà il manager non ha bisogno di avere il model se non per passarlo ai boid actor all'inizio.

## Fasi & Behaviours
1. Fase di Boot: crea gli attori ed aspetta lo start
2. Fase di Update: si fa partire il calcolo delle nuove posizioni
3. Fase di Collect: si raccolgono gli aggiornamenti e si aggiorna la gui
3. Fase di Stopped: quando si ferma, si può fare reset
4. Fase di ripartenza: viene fatta ripartire la simulazione

Per mandare dei messaggi a sé stesso e rimanere reattivo, 
vengono usati più behavior.

Un behaviour è praticamente la lista di messaggi che può gestire, 
degli altri viene fatto lo stash, in modo che vengano gestiti 
appena si arriva ad un behaviour adatto.

Manager Behaviours : 
- Behaviour 0: boot, crea gli attori
- Behaviour 1: update, fa partire il calcolo o reagisce agli eventi della gui, si manda un messaggio per continuare la simulazione ma viene gestisto dopo
- Behaviour 2: collect, raccoglie tutti gli updates ed aggiorna la gui (non reagisce agli eventi della gui)
- Behaviour 4: stopped simulation, si può fare il reset, rimane reattivo agli eventi della gui

Usando più behaviour può mandare a sé stesso dei messaggi ma non gestirli subito, 
per continuare il loop ma fare altre cose prima.

Anche boid actor deve avere più di un behaviour?

