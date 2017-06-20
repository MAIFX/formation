# Langage

http://docs.scala-lang.org/fr/cheatsheets/

## Slides

* https://github.com/mathieuancelin/EMN-2016/blob/master/play2-2.5.pdf
* https://github.com/mathieuancelin/EMN-2016/blob/master/scala.pdf

## Pré requis 

* sbt : 
    * http://www.scala-sbt.org/download.html   
* intellij 
    * https://www.jetbrains.com/idea/download

## Variables

* var
* val
* Déclaration des types 

## Les types de base 

* String
* Int
* Boolean 
* ... 
* String interpolation

## Fonction

* def
* Tout est expression !
* Unit 

## Exercice 1

Présentation de play

Créer une api qui retourne un json

* /messages/:msg 

=> retourne { message: "Hello $msg" }

## Classes et objets

* class
    * variables de classe
    * variables privées 
    * constructeurs altérnatifs 
* case class
* object + companion 
    * méthode apply  
    
Dev avancé : 

* classes abstraites
* trait


## Pattern matching

* Match 
* Destructuration 

## SDK

* Tuples

Structures monadiques 
* Option
* List
    * Cons
    * écrire la fonction map en récursif 
* Try
* Either

=> map, flatMap (et fold ?)


## Exercice 2 : CRUD part 1

CRUD avec un store en mémoire (HashMap)

Api de la forme 

```scala 
def get(id: String): Option[User]
```

* Model sous forme de case class  
    * User avec id, nom, prénom 
* toJson fonction case class => String avec string interpolation  


## Implicits

* lib play json 

## Exercice 3 

* Utiliser play json pour sérialiser les case class

Enrichir l'api :
 
```scala
def create(id: String, data: User): Either[ValidationError, User]
def update(id: String, data: User): Either[ValidationError, User]
def delete(id: String, data: User): Unit
```
* Manipulation d'option 
* Validation avec Either
    * create vérifie que le user n'existe pas 


## Futures / Programmation asynchrone 

* Future scala 
* Execution context 
* Introduction à wsClient 

## Exercice 4 

* Modifier le CRUD avec des futures 
* Utiliser WS client pour appeler une api de stockage clé valeur

## Scala "avancé"

* Call by name
* Lazy val 
* Currying
* Import 

# Programmation réactive détaillée  

* Gestion des pools de threads 
* Composition de futures 
* Streams 

## Exercice 5

* CRUD +++ avec composition d'appels asynchrones 
* SSE ou web socket avec streams 
