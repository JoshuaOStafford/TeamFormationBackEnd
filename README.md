# TeamFormationBackEnd
<b>Java code for Team Formation Platform.</b>

<ul>
<li>Python code running on the backend of the Team Formation Platform provides Main.java with the setup data to store all the users and their partner preferences for forming teams.</li>
<li>Main.java executes the chosen team formation algorithm and writes the results to the console.</li>
<li>Console is read by the Python code and stored in the site's database.</li>
</ul>

<b><u>Algorithms Currently Offered</u></b>
<ul>
<li>Iterative Soulmates*</li>
<li>Random Serial Dictatorship</li>
<li>Heuristic Approach</li>
<li>Rotational Proposer Mechanism</li>
</ul>
* - (pre-processing algorithm, normally used along with one of the algorithms below)
<h2>Iterative Soulmates</h2>
<p>This preprocessing algorithm will find any groups of members of a specified size that meet the Soulmates Criteria</p>
<p><b>Soulmates Criteria:</b>A team of <i>n</i> users is meets the soulmates criteria iff each member of the team considers every other member to be in their top <i>n-1</i> available choices.</p>
<h2>Random Serial Dictatorship</h2>
<p>Users are given a random rank for when they get to propose teams. The member currently proposing is the dictator and gets to form their team without taking into consideration any other user's preferences. Then the next dictator gets to go until all the members have teams.
<h2>Heuristics Approach</h2>
<p>The proposer invites their best available options to join their team. If the average of the available options for the user being invited is better than the team being proposed, that user declines the offer. If all players agree to the team, the team is formed and the next player still teamless proposes a team. Users take turns proposing teams for a finite number of rounds.</p>
<h2>Rotational Proposer Mechanism</h2>
