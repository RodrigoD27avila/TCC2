package br.furb.packing.jenetic;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import static org.jenetics.engine.limit.byExecutionTime;
import static org.jenetics.engine.limit.bySteadyFitness;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;

import org.jenetics.EnumGene;
import org.jenetics.Optimize;
import org.jenetics.PartiallyMatchedCrossover;
import org.jenetics.Phenotype;
import org.jenetics.SwapMutator;
import org.jenetics.engine.Engine;
import org.jenetics.engine.codecs;

import br.furb.common.Polygon;
import br.furb.packing.BottomLeftFillAgorithm;
import br.furb.packing.NFPImplementation;
import br.furb.packing.PackingAlgorithm;
import br.furb.packing.PackingResult;
import br.furb.packing.StopCriteria;
import br.furb.view.ui.IDataChangeListener;

public class JeneticAlgorithm implements PackingAlgorithm {
	
	static Polygon[] mPolygonsList;
	static int mRotationsNumber;
	static double mSheetHeight;
	static NFPImplementation mNFP;
	static int countExecution = 0;
	static Integer syncSemaphore = 0;
	
	// Calculate the path length of the current genotype.
	private static PackingResult fitness(final int[] permutation) {
		
		int localCount = 0;
		synchronized(JeneticAlgorithm.syncSemaphore) {
			localCount = JeneticAlgorithm.countExecution;
			JeneticAlgorithm.countExecution += 1;
		}	
		
		long start = System.currentTimeMillis();
		
		Polygon[] polygonsList = new Polygon[mPolygonsList.length];
		for (int i=0;i<permutation.length;i++) {
			polygonsList[i] = mPolygonsList[permutation[i]]; // check limits
		}
		BottomLeftFillAgorithm bottomLeftFill = new BottomLeftFillAgorithm(mNFP);
		PackingResult ret = bottomLeftFill.doPacking(polygonsList, mRotationsNumber, mSheetHeight);
		long end = System.currentTimeMillis(); 
		
		return ret;
	}

	private IDataChangeListener[] listeners;
				

	public PackingResult doPackingCustom(NFPImplementation nfp, Polygon[] polygonsList, int rotationsNumber, double sheetHeight,
			int generations, int populationSize, int time) {
		
		JeneticAlgorithm.mPolygonsList = polygonsList;
		JeneticAlgorithm.mRotationsNumber = rotationsNumber;
		JeneticAlgorithm.mSheetHeight = sheetHeight;
		JeneticAlgorithm.mNFP = nfp;
		
		int ITEMS_LEN = polygonsList.length;
		int GENERATIONS = generations;
		
		int maxPhenenonAge = (int) (GENERATIONS * 0.2);
		double swapMutatorFactor = 0.2;
		double crossoverFactor = 0.35;
		int steadyFitness = (int) (GENERATIONS * 0.3);
		
		boolean hasMaxPhenenonAge = System.getProperty( "maxPhenenonAge" ) != null;
		boolean hasSwapMutatorFactor = System.getProperty( "swapMutatorFactor" ) != null;
		boolean hasCrossoverFactor = System.getProperty( "crossoverFactor" ) != null;
		boolean hasSteadyFitness = System.getProperty( "steadyFitness" ) != null;
		
		if (hasMaxPhenenonAge) {
			maxPhenenonAge = Integer.parseInt(System.getProperty("maxPhenenonAge"));
			System.out.println("maxPhenenonAge changed to: " + String.valueOf(maxPhenenonAge));
		}
		
		if (hasSwapMutatorFactor) {
			swapMutatorFactor = Double.parseDouble(System.getProperty("swapMutatorFactor"));
			System.out.println("swapMutatorFactor changed to: " + String.valueOf(swapMutatorFactor));
		}
		
		if (hasCrossoverFactor) {
			crossoverFactor = Double.parseDouble(System.getProperty("crossoverFactor"));
			System.out.println("crossoverFactor changed to: " + String.valueOf(crossoverFactor));
		}
		
		if (hasSteadyFitness) {
			steadyFitness = Integer.parseInt(System.getProperty("steadyFitness"));
			System.out.println("steadyFitness changed to: " + String.valueOf(steadyFitness));
		}	
		
		final Engine<EnumGene<Integer>, PackingResult> engine = Engine
				.builder(
						JeneticAlgorithm::fitness,
					codecs.ofPermutation(ITEMS_LEN))
				.optimize(Optimize.MINIMUM)
				.maximalPhenotypeAge(maxPhenenonAge)
				.populationSize(populationSize)
				.alterers(
					new SwapMutator<>(swapMutatorFactor),
					new PartiallyMatchedCrossover<>(crossoverFactor))
				.executor(Executors.newFixedThreadPool(8))
				.build();

			// Create evolution statistics consumer.
			final ByPassStatistic<PackingResult, ?>
				statistics = new ByPassStatistic<>(this);
			
			Phenotype<EnumGene<Integer>, PackingResult> best 
			=
						engine.stream()
						// Truncate the evolution stream after `steadyFitness` "steady" generations.
						.limit(bySteadyFitness(steadyFitness))
						// The evolution will stop after maximal time.
						.limit(byExecutionTime(Duration.ofMillis(time), Clock.systemUTC()))
						// The evolution will stop after maximal GENERATIONS generations.
						.limit(GENERATIONS)
						// Update the evaluation statistics after each generation
						.peek(statistics)
						// Collect (reduce) the evolution stream to its best phenotype.
						.collect(toBestPhenotype());
						

			System.out.println(statistics);
			System.out.println(best.getFitness().getHeight());
					
		
		return best.getFitness();
	}

	public void addLisneter(IDataChangeListener[] listeners) {
		this.listeners = listeners;
	}

	public void notifyListeners(PackingResult result) {
//		for (IDataChangeListener listener : listeners) {
//			if (listener != null) {
//				listener.notifyChanged(result);
//			}
//		}
	}

	@Override
	public PackingResult doPacking(NFPImplementation nfp, Polygon[] polygonsList, int rotationsNumber,
			double sheetHeight, StopCriteria stopCriteria, int stopValue) {
		
		final int DEFAULT_POPULATION_SIZE = 100;		

			if (stopCriteria.equals(stopCriteria.LOOP)) {
				return doPackingCustom(nfp, polygonsList, rotationsNumber, sheetHeight, stopValue, DEFAULT_POPULATION_SIZE, 1200000);
			} else {
				return doPackingCustom(nfp, polygonsList, rotationsNumber, sheetHeight, 9999, DEFAULT_POPULATION_SIZE, stopValue);			
			}				
		

	}
}
