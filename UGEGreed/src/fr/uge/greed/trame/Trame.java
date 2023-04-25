package fr.uge.greed.trame;

public sealed interface Trame permits 
TrameAnnonceIntentionDeco,TramePingConfirmationChangementCo,TrameSuppression,
TrameFirstRoot,
TrameFirstLeaf,
TrameNewLeaf,
TrameFullTree,
TramePingEnvoi,
TramePingReponse
{
	/*
	0 1 2 DUMP
	TrameAnnonceIntentionDeco = 3
	TramePingConfirmationChangementCo = 4
	TrameSuppression = 5
	TrameFirstRoot = 6
	TrameFirstLeaf = 7
	TrameFullTree = 8
	TrameNewLeaf = 9
	TramePingEnvoi = 10
	TramePingReponse = 11
	*/
	public int getOp();
}
