# OhHell
An online Oh Hell card game



<br></br>
<b>How to use the server</b>

To run the server, make sure you have port forwarding setup so that requests for the port you want to use get sent to the machine running the server. Note that with some router brands you need to use the local IP address of the machine running the server (as opposed to the public IP address) as the host name in the client if you are on the same network as that machine. If you are running a client on the same machine as the server, you can just use "localhost" as the host name.



<br></br>
<b>Rules of Oh Hell</b>

Oh Hell is a trick taking card game. Every round, cards are dealt to all players, and the top card of the kitty is flipped up to signify the trump suit. Starting with the player left of the dealer and going around clockwise, each player bids how many tricks they think they will take. Each player can bid any whole number between 0 and the number of cards dealt per hand that round, inclusive, except for the dealer, who cannot bid the number such that everyone's bids would sum to the number of cards per hand. Then the player left of the dealer leads the first trick. Each player must follow suit if possible and is not required to trump or play a card that beats the cards in the trick so far.

At the end of the round, the players are scored depending on their bid and the number of tricks they took. If someone bid n and took n, then that person gains 10 + n^2 points. If someone bid n and took m, where m is not equal to n, then that person loses 5 * (1 + 2 + ... + |n-m|) points. Yes, this is the correct way to score Oh Hell.

When the round ends, the player to the left of the dealer becomes the new dealer. The number of cards per hand also depends on the round. The number of cards in the first round is min(10, 51/n), where n is the number of players. In each subsequent round, the number of cards goes down by 1, until that number is 1. At that point, every player deals a round with 1 card per hand. Afterward, the number of cards increases by 1 each round, until that number is back to the number of cards in the first round. After that, the game is over, and the player with the highest score wins.
