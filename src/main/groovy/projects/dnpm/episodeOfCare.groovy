package projects.myproject


/**
 *  ############### MTB-Behandlungs-Episode ###############
 *
 * Represented by a CXX ???
 * Specified by ????
 *
 * @author Jaqueline Patzek
 * @since CXX.v.3.17.0.2
 */

episodeOfCare { // <<< nicht bekannte entitaet?

  // id
  id = "Episode/Episode-" + context.source["patientcontainer.id"]


  period {
    start = new Date(); // todo
  }

}


