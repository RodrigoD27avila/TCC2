package br.furb.packing;

import java.util.Arrays;

import br.furb.common.Polygon;
import br.furb.packing.genetic.GeneticAlgorithm;
import br.furb.view.ui.IDataChangeListener;

public class PackingExecutor {

	public PackingResult executePacking(Polygon[] polygons, double height, //
			int rotations, StopCriteria stopCriteria, int stopValue, LocalSearch localSearch,//
			IDataChangeListener... listeners) {

		PackingAlgorithm algorithm;
		if (localSearch == LocalSearch.HILL_CLIMBING) {
			algorithm = new HillClimbingAlgorithm();
		} else if (localSearch == LocalSearch.TABU_SEARCH){
			algorithm = new TabuSearch();
		} else {
			algorithm = new GeneticAlgorithm();
		}

		algorithm.addLisneter(listeners);

		Arrays.sort(polygons, new Polygon.HeightComparator());
		return algorithm.doPacking(polygons, rotations, height, stopCriteria, stopValue);
	}

}