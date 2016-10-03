package ch.muriz.gaface;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Population {

    // Number of Individuals
    private final int populationSize = 30;
    // [0, infinity)
    private final float populationMutationRate = 0.1f;
    // (0, 1] : Fraction of population in tournament
    private final float tournamentFraction = 0.6f;
    // [0, infinity)
    private final float populationCrossoverRate = 0.9f;
    // Best fitness found in a generation so far
    private int bestFitness = 0;
    // Location to store status information
    final String statusDirection = "/Users/Muriz/Desktop/face_test";
    // File which holds the last generation's DNA
    private final String sourceGenerationFile = null;

    List<Individual> individuals = new ArrayList<Individual>();

    Random rand = new Random();
    Fitness fitness;

    public Population(Population pop) {
        if(pop == null) populationFromScratch(); // Create new random population
    }

    /*
     * Creates new random population
     */
    private void populationFromScratch() {
        for(int i = 0; i < getPopulationSize(); i++) {
            Individual newIndividual = new Individual(null);
            saveIndividual(newIndividual);
        }
    }

    /*
     * Save individual
     */
    private void saveIndividual(Individual indiv) {individuals.add(indiv);}

    public List<List<Integer>> getDNAList() {
        List<List<Integer>> DNAList = new ArrayList<>();
        if(individuals.size() != 0)
            for(Individual indiv : individuals)
                DNAList.add(indiv.getDNA());
        else System.out.println("There are no individuals");

        return DNAList;
    }

    // Evolve a population
    public Population evolvePopulation(Population pop, boolean save, int populationNumber) {
        fitness = new Fitness();
        List<List<Integer>> DNAList = pop.getDNAList();
        List<Double> fitnessList = new ArrayList<>();
        for(List<Integer> list : DNAList) {
            try {
                // slow as f*ck
                fitnessList.add(fitness.calculateFitness(list));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //System.out.println(fitnessList);
        List<List<Number>> individualFitness = new ArrayList<>();
        List<Integer> index = new ArrayList<>();
        List<Double> fitness = new ArrayList<>();
        List<Number> mergedList = new ArrayList<>();
        for(int i = 0; i < fitnessList.size(); i++) {
            index.add(i);
            fitness.add(fitnessList.get(i));
        }
        for(int index1 : index) {
            for(double index2 : fitness) {
                mergedList.add(index2);
                mergedList.add(index1);
            }
        }
        individualFitness = Utils.choppedList(mergedList, 2);
        Collections.sort(individualFitness, new Comparator<List<Number>>() {
            public int compare(List<Number> list1, List<Number> list2) {
                //Simple comparison here
                return Double.compare((double)list1.get(0), (double)list2.get(0));
            }
        });
        Collections.reverse(mergedList);

        // If requested, give a status update with the most fit individual from the old population
        // Also save the DNA for all individuals in the last population, and possible best generation
        if(save) {
            try {
                Fitness newFitness = new Fitness();
                List<Number> individualIndex = individualFitness.get(0);
                Individual exampleIndividual = pop.individuals.get((int)individualIndex.get(1));
                double exampleIndividualFitness = newFitness.calculateFitness(exampleIndividual.getDNA());
                Phenotype phenotype = new Phenotype();
                BufferedImage phenotypeImage = phenotype.createPhenotype(exampleIndividual.getDNA());
                ImageIO.write(phenotypeImage, "png", new File(statusDirection + "/" + populationNumber + ".png"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for(int i = 0; i < (populationSize /2); i++) {
            List<List<Integer>> matingDNA = new ArrayList<>();
            List<Number> matingPair = tournamentSelection(individualFitness);
            for(Number individualNumber : matingPair) {
                matingDNA.add(pop.getDNAList().get((int)individualNumber));
            }

            //Crossover the mating pair's DNA so many times according to crossover rate
            int crossovers = 0;
            if(populationCrossoverRate > 1.0f) {
                while(populationCrossoverRate - crossovers > 1) {
                    matingDNA = crossover(matingDNA);
                    crossovers++;
                }
            }
            if(Math.random() <= populationCrossoverRate - crossovers) {
                matingDNA = crossover(matingDNA);
            }

            // Mutate each mate's DNA so many times accorind to mutation rate
            for(int n = 0; n < 2; n++) {
                int mutations = 0;
                if(populationMutationRate > 1) {
                    while(populationMutationRate - mutations > 1) {
                        matingDNA.set(n, mutate(matingDNA.get(n)));
                        mutations++;
                    }
                }
                if(Math.random() <= populationMutationRate -mutations) {
                    matingDNA.set(n, matingDNA.get(n));
                }
            }

            // Add a new individual based on the newly mutated/crossed over DNA to the population
            for(List<Integer> DNA : matingDNA) {
                Individual newIndiv = new Individual(DNA);
                pop.individuals.add(newIndiv);
            }
        }
        return pop;
    }

    public List<List<Integer>> getSpecificDNAList(int index) {
        List<List<Integer>> specificDNAList = new ArrayList<>();
        if(individuals.size() != 0) specificDNAList.add(individuals.get(index).getDNA());
        else System.out.println("The specific individual doesn't exist");

        return specificDNAList;
    }

    /*
     * Determines what individuals to mate
     * Based on a list of (fitness, individualNumber) tuples
     * Uses tournament selection
     */
    private List<Number> tournamentSelection(List<List<Number>> individualFitness) {
        int tournamentSize = (int)Math.ceil(individualFitness.size() * tournamentFraction);
        if(tournamentSize == 1) tournamentSize = 2;

        // Create tournament
        int listCount = individualFitness.size();
        List<List<Number>> tournament = new ArrayList<>();
        while(tournament.size() < tournamentSize) {
            int chosenIndividual = rand.nextInt(listCount);
            if(!tournament.contains(individualFitness.get(chosenIndividual))) {
                tournament.add(individualFitness.get(chosenIndividual));
            }
        }
        // Return two most fit individuals from tournament
        Collections.sort(tournament, new Comparator<List<Number>>() {
            public int compare(List<Number> list1, List<Number> list2) {
                //Simple comparison here
                return Double.compare((double)list1.get(0), (double)list2.get(0));
            }
        });
        Collections.reverse(tournament);
        List<Number> floatList1 = tournament.get(0);
        List<Number> floatList2 = tournament.get(1);
        List<Number> resultList = new ArrayList<>();
        resultList.add(floatList1.get(1)); resultList.add(floatList2.get(1));
        return resultList;
    }

    /*
     * Crosses over two pieces of mating DNA
     */
    private List<List<Integer>> crossover(List<List<Integer>> matingDNA) {
        int pivot = rand.nextInt(matingDNA.get(0).size());
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> firstMatingDNA = matingDNA.get(0);
        List<Integer> secondMatingDNA = matingDNA.get(1);
        List<Integer> firstPart = firstMatingDNA.subList(0, firstMatingDNA.size()-pivot);
        List<Integer> secondPart = secondMatingDNA.subList(secondMatingDNA.size()-pivot, secondMatingDNA.size());
        ArrayList<Integer> firstResult = new ArrayList<>(firstPart);
        firstResult.addAll(secondPart);
        result.add(firstResult);

        List<Integer> firstPartList2 = secondMatingDNA.subList(0, secondMatingDNA.size()-pivot);
        List<Integer> secondPartList2 = firstMatingDNA.subList(firstMatingDNA.size()-pivot, firstMatingDNA.size());
        ArrayList<Integer> secondResult = new ArrayList<>(firstPartList2);
        secondResult.addAll(secondPartList2);
        result.add(secondResult);
        return result;
    }

    /*
     * Randomly mutates a piece of DNA
     */
    private List<Integer> mutate(List<Integer> DNA) {
        int chosenBase = rand.nextInt(DNA.size());
        int chosenBaseType = Individual.INDIVIDUAL_BASE_TYPES[rand.nextInt(Individual.INDIVIDUAL_BASE_TYPES.length)];
        DNA.set(chosenBase, chosenBaseType);
        return DNA;
    }

    // Get population size
    public int getPopulationSize() {return populationSize;}
}
