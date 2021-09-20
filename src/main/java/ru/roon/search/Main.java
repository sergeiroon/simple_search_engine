package ru.roon.search;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static Scanner scanner = new Scanner(System.in);

    private static Map<Integer, String> directory = new HashMap<>();
    private static Map<String, List<Integer>> invertedIndex = new HashMap<>();

    public static void main(String[] args) {
        String fileName = retrieveFileName(args);
        fillDirectory(fileName);
        fillInInvertedIndex(fileName);
        int selectedItem = selectMenu();
        while (selectedItem != 0) {
            switch (selectedItem) {
                case 1:
                    searchPeople(fileName);
                    break;
                case 2:
                    printAllPeople(fileName);
                    break;
                default:
                    System.out.println("Incorrect option! Try again.");
                    break;
            }
            selectedItem = selectMenu();
        }
        System.out.println("Buy!");
    }

    private static void fillDirectory(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            AtomicInteger count = new AtomicInteger(0);
            br.lines().forEach(line -> {
                directory.put(count.get(), line);
                count.incrementAndGet();
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fillInInvertedIndex(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            AtomicInteger count = new AtomicInteger(0);
            br.lines().forEach(line -> {
                String[] words = line.split(" ");
                Arrays.stream(words).forEach(word -> {
                    List<Integer> lines = invertedIndex.get(word.trim().toLowerCase());
                    if (lines == null) {
                        lines = new ArrayList<>();
                        lines.add(count.get());
                        invertedIndex.put(word.trim().toLowerCase(), lines);
                    } else {
                        lines.add(count.get());
                    }
                });
                count.incrementAndGet();
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printAllPeople(String file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.lines().forEach(System.out::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void searchPeople(String file) {
        System.out.println("Select a matching strategy: ALL, ANY, NONE");
        String strategy = scanner.next().trim().toLowerCase();
        SearchContext searchContext = new SearchContext();
        SearchStrategy searchStrategy = null;
        switch (strategy) {
            case "any":
                searchStrategy = new SearchAny(invertedIndex, directory);
                break;
            case "all":
                searchStrategy = new SearchAll(invertedIndex, directory);
                break;
            case "none":
                searchStrategy = new SearchNone(invertedIndex, directory);
                break;
            default:
                System.out.println("wrong strategy!");
        }
        searchContext.setStrategy(searchStrategy);
        System.out.println("Enter a name or email to search all suitable people.");
        scanner.nextLine();
        String searchString = scanner.nextLine();
        searchContext.search(searchString);
        /*AtomicBoolean searched = new AtomicBoolean(false);
        List<Integer> indexes = invertedIndex.get(searchString.trim().toLowerCase());
        if (indexes == null) {
            System.out.println("No matching people found.");
        } else {
            indexes.forEach(index -> {
                System.out.println(directory.get(index));
            });
        }*/
    }

    private static int selectMenu() {

        System.out.println("=== Menu ===");
        System.out.println("1. Find a person");
        System.out.println("2. Print all people");
        System.out.println("0. Exit");
        return scanner.nextInt();
    }


    private static String retrieveFileName(String[] args) {
        if (args.length > 0) {
            String nameArgument = args[0];
            if (nameArgument.equals("--data")) {
                return args[1];
            }
        }
        return null;
    }
}

abstract class SearchStrategy {

    public SearchStrategy(Map<String, List<Integer>> invertedIndex, Map<Integer, String> directory) {
        this.invertedIndex = invertedIndex;
        this.directory = directory;
    }

    protected Map<Integer, String> directory;
    protected Map<String, List<Integer>> invertedIndex;
    protected Set<Integer> searchedIds = new HashSet<>();

    abstract void search(String query);

    public void printSearched() {
        if (searchedIds.isEmpty()) {
            System.out.println("No matching people found.");
        } else {
            searchedIds.forEach(index -> {
                System.out.println(directory.get(index));
            });
        }
    }
}

class SearchAny extends SearchStrategy {

    public SearchAny(Map<String, List<Integer>> invertedIndex, Map<Integer, String> directory) {
        super(invertedIndex, directory);
    }

    @Override
    void search(String query) {

        String[] words = query.split(" ");
        for (String str : words) {
            searchedIds.addAll(invertedIndex.get(str.trim().toLowerCase()));
        }
    }
}

class SearchAll extends SearchStrategy {

    public SearchAll(Map<String, List<Integer>> invertedIndex, Map<Integer, String> directory) {
        super(invertedIndex, directory);
    }

    @Override
    void search(String query) {

        String[] words = query.split(" ");

        for (String str : words) {
            if (invertedIndex.get(str.trim().toLowerCase()) != null) {
                if (searchedIds.isEmpty()) {
                    searchedIds.addAll(invertedIndex.get(str.trim().toLowerCase()));
                } else {
                    searchedIds.retainAll(invertedIndex.get(str.trim().toLowerCase()));
                }
            }

        }
    }
}

class SearchNone extends SearchStrategy {

    public SearchNone(Map<String, List<Integer>> invertedIndex, Map<Integer, String> directory) {
        super(invertedIndex, directory);
    }

    @Override
    void search(String query) {
        String[] words = query.split(" ");
        Set<Integer> excludedIds = new HashSet<>();

        for (String str : words) {
            if (invertedIndex.get(str.trim().toLowerCase()) != null) {
                excludedIds.addAll(invertedIndex.get(str.trim().toLowerCase()));
            }
        }
        Set<Integer> includedIds = directory.keySet();
        includedIds.removeAll(excludedIds);
        searchedIds.addAll(includedIds);
    }
}

class SearchContext {
    private SearchStrategy strategy;

    public SearchStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(SearchStrategy strategy) {
        this.strategy = strategy;
    }

    public void search(String query) {
        strategy.search(query);
        strategy.printSearched();
    }
}