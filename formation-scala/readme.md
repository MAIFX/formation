#Langage

http://docs.scala-lang.org/fr/cheatsheets/

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


## Classes et objets

* class
* case class
* object
* trait
* classes abstraites


## SDK

* Tuples
* Option
* List
* Either
* Try
* Map 

## Pattern matching

* Match 
* Destructuration 

## Exercice 2

CRUD avec un store de type Map 

* Données en case class 
* Manipulation d'option 
* toJson fonction case class => String avec string interpolation  


## Implicits

* lib play json 

## Exercice 3 

* Utiliser play json pour sérialiser les case class
 
## Monades 

* map / flatMap 
* for comprehension 

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