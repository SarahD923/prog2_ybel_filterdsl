# Aufgabe 4: Visitor-Pattern und Pattern Matching

Beide Varianten erzeugen aus demselben Parse-Tree den gleichen AST. Der
Unterschied liegt hauptsächlich darin, wie der Baum durchlaufen und das Ergebnis
gespeichert wird.

## Visitor-Pattern

Beim Visitor werden die von ANTLR erzeugten `visit`-Methoden überschrieben. Jede
Methode ist für eine Regel der Grammatik zuständig. Zwischenergebnisse werden in
Stacks für Ausdrücke, Werte und Wertelisten gespeichert.

Vorteile:

- Die Struktur der Visitor-Klasse passt direkt zu den Regeln der Grammatik.
- Die rekursive Traversierung wird größtenteils von ANTLR übernommen.
- Eine neue Verarbeitung des Parse-Trees kann als weiterer Visitor ergänzt
  werden, ohne die Parser-Klassen zu verändern.

Nachteile:

- Durch die Stacks besitzt der Visitor einen veränderlichen Zustand.
- Man muss darauf achten, in welcher Reihenfolge Werte vom Stack genommen werden.
- Der Datenfluss ist schwieriger nachzuvollziehen, weil die Methoden `Void`
  zurückgeben.

## Pattern Matching

Bei der Pattern-Matching-Variante werden die Kindknoten direkt über die
Kontext-Methoden gelesen. Die Methoden liefern den aufgebauten Teilausdruck als
Rückgabewert. Mit `switch` und Type Patterns wird zwischen verschiedenen
Kontexttypen unterschieden.

Vorteile:

- Es wird kein veränderlicher Zustand und kein Stack benötigt.
- Der Rückgabewert jeder Methode zeigt direkt, welcher AST-Knoten erzeugt wird.
- Für kleinere Grammatiken ist der Ablauf kompakt und gut lesbar.

Nachteile:

- Die Traversierung muss vollständig selbst programmiert werden.
- Der Code hängt direkt von der Struktur der ANTLR-Kontexte ab.
- Bei einer größeren oder häufig veränderten Grammatik müssen möglicherweise
  viele `switch`-Stellen angepasst werden.

## Fazit

Für diese kleine Grammatik finde ich Pattern Matching übersichtlicher, weil die
Methoden ohne Zustand auskommen. Das Visitor-Pattern ist dafür näher an ANTLR und
kann bei einer größeren Grammatik besser strukturiert werden. Die Tests zeigen,
dass beide Implementierungen dieselben ASTs erzeugen.
