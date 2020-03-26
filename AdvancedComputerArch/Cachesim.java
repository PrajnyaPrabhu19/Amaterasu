package main.java;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Cachesim {
    private static String filename;
    private static int way;
    private static int total_cache;
    private static int c_block;

    public static void main(String[] args) {
        if(args.length==0 || args.length<4){
            System.out.println("Please enter the arguments while running this file in format:");
            System.out.println("java Program_Name.java Trace_File_Name/Path total_cache_in_bytes  cache_block_size way");
            System.exit(1);
        }
        filename = args[0];
        total_cache = Integer.parseInt(args[1]);
        c_block = Integer.parseInt(args[2]);
        way = Integer.parseInt(args[3]);
        Scanner in = new Scanner(System.in);
        while(total_cache>4194304){
            System.out.println("Enter the total cache size in bytes (less than equal to 4MB):");
            total_cache = in.nextInt();
        }

        //fully associative
        if(way==0){
            fullyAssociative();
        }
        //direct
        else if(way==1){
            direct();
        }
        //2-way associative
        else if(way==2){
            twoWay();
        }
        //4-way associative
        else if(way==4){
            fourWay();
        }
        else{
            System.out.println("No such way as "+way);
            System.out.println("Please enter a valid value and run the program again. Ways supported: (0:fully associative | 1: direct | 2: 2-way associative | 4: 4-way associative)");
        }
    }

    private static void fourWay() {
        int c_offset = findOffset();
        int c_index = findIndex(4);
        int tag = 32 - c_index-c_offset;
        int miss=0;
        int counter =0;
        int maxSize = total_cache/(4*c_block);
        //Hashmap to contain the index and the cache address values. only 4 tag address allowed for each index
        HashMap<String, ArrayList<String>> fourWay = new HashMap<>();
        HashMap<Integer, String> lru = new HashMap<>();
        int min=0, max =0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                counter++;
                String[] arrOfTrace = line.split(" ");
                int offset = Integer.parseInt(arrOfTrace[1]);
                //fetch the hexadecimal address and get the 32 LSB to be used as cache address.
                String binary_address = getBinaryAddressofLocal(arrOfTrace[2], offset, 32);
                String ind_key = binary_address.substring(tag,tag+c_index);
                String tag_addr = binary_address.substring(0,tag);
                //check if the index is already in cache and check the tag
                if(fourWay.containsKey(ind_key)){
                    //check for the tag address
                    ArrayList<String> values = fourWay.get(ind_key);
                    //check if tag address is present in the block line
                    if(values.contains(tag_addr)){
                        //check if the tag address if found at index 0-3, if yes then put it in the end.
                       int ac_index = values.indexOf(tag_addr);
                       values.remove(ac_index);
                       values.add(tag_addr);
                       // add the updated list back to map
                       fourWay.put(ind_key,values);
                    }
                    else{
                        //increment miss as the tag address was not present in the column.
                        miss++;
                        //if the block line is already full, remove the first item and add new item to the list
                        if(values.size()==4){
                            values.remove(0);
                            values.add(tag_addr);
                            fourWay.put(ind_key,values);
                        }
                        //else just add the new tag address
                        else{
                            values.add(tag_addr);
                            fourWay.put(ind_key,values);
                        }
                    }
                    //increment the value in lru
                    lru = incrementValue(lru,ind_key,max);
                    max++;
                    min++;
                }
                //new index
                else{
                    miss++;
                    lru.put(max,ind_key);
                    max++;
                    //check for the hashmap size
                    if(fourWay.size()==maxSize){
                        //remove the item from lru
                        String rem_index = lru.get(min);
                        lru.remove(min);
                        min++;
                        fourWay.remove(rem_index);
                        ArrayList<String> addresses = new ArrayList<>();
                        addresses.add(tag_addr);
                        fourWay.put(ind_key,addresses);
                    }
                    else{
                        ArrayList<String> addresses = new ArrayList<>();
                        addresses.add(tag_addr);
                        fourWay.put(ind_key,addresses);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        printValues(miss, c_index, c_offset, tag, counter, maxSize);
    }

    private static void twoWay() {
        int c_offset = findOffset();
        int c_index = findIndex(2);
        int tag = 32 - c_index -c_offset;
        int miss=0;
        int counter =0;
        int maxSize = total_cache/(2*c_block);
        //Hashmap to contain the index and the cache address values. only 2 address allowed for each index
        HashMap<String, ArrayList<String>> twoWay = new HashMap<>();
        HashMap<Integer, String> lru = new HashMap<>();
        int min=0, max =0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                counter++;
                String[] arrOfTrace = line.split(" ");
                int offset = Integer.parseInt(arrOfTrace[1]);
                //fetch the hexadecimal address and get the 32 LSB to be used as cache address.
                String binary_address = getBinaryAddressofLocal(arrOfTrace[2], offset, 32);
                String ind_key = binary_address.substring(tag,tag+c_index);
                String tag_addr = binary_address.substring(0,tag);
                //check if the index is already in cache and check the tag
                if(twoWay.containsKey(ind_key)){
                    //check for the tag address
                    ArrayList<String> values = twoWay.get(ind_key);
                    //check if tag address is present in the block line
                    if(values.contains(tag_addr)){
                        //check if the tag address if found at index 0, if yes then swap it.
                        if(values.get(0).equals(tag_addr)){
                            if(values.size()==2){
                                values.set(0,values.get(1));
                                values.set(1, tag_addr);
                                twoWay.put(ind_key,values);
                            }
                        }
                    }
                    else{
                        //increment miss as the tag address was not present in the column.
                        miss++;
                        //if the block line is already full, remove the first item and add new item to the list
                        if(values.size()==2){
                            String tag1 = values.get(1);
                            values.remove(0);
                            values.add(0,tag1);
                            values.add(1,tag_addr);
                            twoWay.put(ind_key,values);
                        }
                        //else just add the new tag address
                        else{
                            values.add(tag_addr);
                            twoWay.put(ind_key,values);
                        }
                    }
                    //increment the value in lru
                    lru = incrementValue(lru,ind_key,max);
                    max++;
                    min++;
                }
                //new index
                else{
                    miss++;
                    lru.put(max,ind_key);
                    max++;
                    //check for the hashmap size
                    if(twoWay.size()==maxSize){
                        //remove the item from lru
                        String rem_index = lru.get(min);
                        lru.remove(min);
                        min++;
                        twoWay.remove(rem_index);
                        ArrayList<String> addresses = new ArrayList<>();
                        addresses.add(tag_addr);
                        twoWay.put(ind_key,addresses);
                    }
                    else{
                        ArrayList<String> addresses = new ArrayList<>();
                        addresses.add(tag_addr);
                        twoWay.put(ind_key,addresses);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        printValues(miss, c_index, c_offset, tag, counter, maxSize);
    }

    private static void direct() {
        //use arraylist instead of map for lru
        int c_offset = findOffset();
        int c_index = findIndex(1);
        int tag = 32-c_index-c_offset;
        int miss =0;
        int counter =0;
        //have a map with index as the key and tag as the address
        HashMap<String,String> direct = new HashMap<>();
        HashMap<Integer, String> lru = new HashMap<>();
        int min =0;
        int max = 0;
        int maxSize = total_cache/c_block;
        //read the trace file line by line
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                counter++;
                String[] arrOfTrace = line.split(" ");
                int offset = Integer.parseInt(arrOfTrace[1]);
                //fetch the hexadecimal address and get the 32 LSB to be used as cache address.
                String binary_address = getBinaryAddressofLocal(arrOfTrace[2], offset, 32);
                String ind_key = binary_address.substring(tag, (32-c_offset));
                String tag_addr = binary_address.substring(0,tag);
                //check if the index is already in cache and check the tag
                if(direct.containsKey(ind_key)){
                    String tag_address = direct.get(ind_key);
                    if(!tag_address.equals(tag_addr)){
                        miss++;
                        direct.put(ind_key,tag_addr);
                        //increment the lru state
                        lru = incrementValue(lru, ind_key, max);
                        min++;
                        max++;
                    }
                    lru = incrementValue(lru, ind_key, max);
                    min++;
                    max++;
                }
                else{
                    miss++;
                    if(direct.size()==maxSize){
                        String remove = lru.get(min);
                        min++;
                        lru.put(max,ind_key);
                        max++;
                    }
                    else{
                        direct.put(ind_key,tag_addr);
                        lru.put(max,ind_key);
                        max++;
                    }
                }
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        printValues(miss, c_index,c_offset,tag,counter, maxSize);
    }

    private static void fullyAssociative() {
        int c_index = 0;
        int c_offset = findOffset();
        int tag = 32-c_offset;
        //LL to implement the fully associative cache
        LinkedList<String> full_associative = new LinkedList<>();
        //Hashmap to have the history about lru
        HashMap<Integer, String> lru = new HashMap<>();
        int miss = 0;
        int counter =0;
        int maxSize = total_cache/c_block;
        int min=0;
        int max =0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                counter++;
                String[] arrOfTrace = line.split(" ");
                int offset = Integer.parseInt(arrOfTrace[1]);
                //fetch the hexadecimal address and get the 32 LSB to be used as cache address.
                String binary_address = getBinaryAddressofLocal(arrOfTrace[2], offset, 32);
                //We dont need the offset from cache address
                String tag_addr = binary_address.substring(0,tag);
                //check if the ll has the address
                if(full_associative.contains(tag_addr)){
                    //increment the lru bit
                    lru = incrementValue(lru, tag_addr, max);
                    max++;
                    min++;
                }
                //if the ll does not contain the item yet
                else{
                    miss++;
                    //check if the ll has already reached max
                    if(full_associative.size()==maxSize){
                        //get the lru item from hashmap
                        String remove = lru.get(min);
                        //find the index at which the tag address is present in ll
                        int remove_index = full_associative.indexOf(remove);
                        //replace the old item with new one in ll
                        full_associative.set(remove_index,tag_addr);
                        //remove the item from lru and insert the new tag address with max value
                        lru.remove(remove_index);
                        lru.put(max, tag_addr);
                        max++;
                        min++;
                    }
                    else{
                        full_associative.add(tag_addr);
                        lru.put(max,tag_addr);
                        max++;
                    }
                }
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        printValues(miss,c_index,c_offset,tag,counter,1);
    }

    private static void printValues(int miss, int index, int offset, int tag, int total,int sets) {
        float missRate = (float) miss / (float) total;
        float hitRate = 1-missRate;
        System.out.println("Miss Rate: "+missRate);
        System.out.println("Hit Rate: "+hitRate);
        System.out.println("# of sets: "+sets);
        System.out.println("# of ways: "+way);
        System.out.println("# of tag bits: "+tag);
        System.out.println("# of index bits: "+index);
        System.out.println("# of offset bits: "+offset);
    }

    //method to increment the LRU bit of index/addresses which are not accessed recently
    private static HashMap<Integer, String> incrementValue(HashMap<Integer, String> lru, String ind_key, int max) {
        //find the key value of the address bits
        int val = 0;
        for(Map.Entry<Integer, String> entry:lru.entrySet()){
            if(ind_key.equals(entry.getValue())){
                val=entry.getKey();
            }
        }
        //increment the value of key in lru for all values which are lesser than the current val
        HashMap<Integer,String> newLru = new HashMap<>();
        for(Map.Entry<Integer, String> entry:lru.entrySet()){
            if(entry.getKey()<val){
                newLru.put(entry.getKey()+1, entry.getValue());
            }
            else if(entry.getKey()==val){
                newLru.put(max,entry.getValue());
            }
            else{
                newLru.put(entry.getKey(),entry.getValue());
            }
        }
        return newLru;
    }

    private static int findIndex(int n) {
        int index = total_cache / (n*c_block);
        return (int) (Math.log10(index)/Math.log10(2));
    }

    private static int findOffset() {
        return (int)(Math.log10(c_block)/Math.log10(2));
    }

    private static String getBinaryAddressofLocal(String s, int offset, int lsb) {
        Long num = Long.parseLong(s,16);
        Long address = num+offset;
        String binary = Long.toBinaryString(address);
        int len = binary.length();
        if(len>lsb){
            int diff = len-lsb;
            binary = binary.substring(diff);

        }
        if(len<lsb){
            //add 0 prefixes
            for(int i=0;i<lsb-len;i++){
                binary = '0'+binary;
            }
        }
        return binary;
    }

}