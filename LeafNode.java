import java.util.Arrays;

/**
 * Leaf node of the B+ tree
 */
public class LeafNode extends Node{
    private LeafNode leftSibling;
    private LeafNode rightSibling;
    private int minPairs;
    private int maxPairs;
    private int curNumPairs;
    private Pair[] pairs;

    public LeafNode(int m, Pair pair){
        this.curNumPairs = 0;
        this.minPairs = (int)(Math.ceil(m / 2.0) - 1);
        this.maxPairs = m - 1;
        this.pairs = new Pair[m];
        this.addPair(pair);
    }

    public LeafNode(int m, Pair[] pairs, IndexNode parent){
        this.pairs = pairs;
        this.parent = parent;
        this.maxPairs = m - 1;
        this.minPairs = (int)(Math.ceil(m / 2.0) - 1);
        for(int i = 0; i < pairs.length; i++){
            if(pairs[i] == null){
                this.curNumPairs = i;
                break;
            }
        }

    }

    /**
     * getter for leftSibling variable
     * @return - LeafNode reference
     */
    public LeafNode getLeftSibling() {
        return leftSibling;
    }

    /**
     * setter for leftSibling variable
     * @param leftSibling - LeafNode reference
     */
    public void setLeftSibling(LeafNode leftSibling) {
        this.leftSibling = leftSibling;
    }

    /**
     * getter for rightSibling variable
     * @return - LeafNode reference
     */
    public LeafNode getRightSibling() {
        return rightSibling;
    }

    /**
     * setter for rightSibling variable
     * @param rightSibling - LeafNode reference
     */
    public void setRightSibling(LeafNode rightSibling) {
        this.rightSibling = rightSibling;
    }

    /**
     * getter for curNumPairs variable
     * @return - int value of curNumPairs
     */
    public int getCurNumPairs() {
        return curNumPairs;
    }

    /**
     * setter for curNumPairs variable
     * @param curNumPairs - int value for curNumPairs
     */
    public void setCurNumPairs(int curNumPairs) {
        this.curNumPairs = curNumPairs;
    }

    /**
     * getter for pairs variable
     * @return - array of Pair references
     */
    public Pair[] getPairs() {
        return pairs;
    }

    /**
     * Adds a pair to the leaf node
     * @param pair - The pair to be added
     * @return - true if pair is added to the leaf node successfully otherwise false
     */
    public boolean addPair(Pair pair){
        if(curNumPairs == maxPairs){
            return false;
        }
        this.pairs[curNumPairs++] = pair;
        this.sortPairs();
        return true;
    }

    /**
     * Sorts all the dictionary pairs.
     */
    public void sortPairs(){
        Arrays.sort(this.pairs, (a, b) ->{
           if(a == null && b == null) {
               return 0;
           }else if(a == null){
               return 1;
           }else if(b == null){
               return -1;
           }else{
               return a.key - b.key;
           }
        });
    }

    /**
     * Deletes a pair from the leaf node
     * @param idx - the index of the pair to be deleted
     */
    public void deletePair(int idx){
        this.pairs[idx] = null;
        this.curNumPairs--;
    }

    /**
     * Binary search for a pair and return the index of the pair if found.
     * @param key - key of the pair to be searched
     * @return - integer value -1 if the key is not found or else, the index of the pair which has the target key
     */
    public int search(int key){
        int l = 0, r = this.curNumPairs - 1;
        while(l <= r){
            int mid = l + ( r - l ) / 2;
            if(this.pairs[mid].key == key){
                return mid;
            }else if(this.pairs[mid].key > key){
                r = mid - 1;
            }else{
                l = mid + 1;
            }
        }
        return -1;
    }

    /**
     * Check if the leaf node is deficient
     * @return - boolean value
     */
    public boolean isDeficient(){
        return this.curNumPairs < this.minPairs;
    }

    /**
     * Check if the leaf node can lend a pair to the sibling
     * @return - boolean value
     */
    public boolean canLendAPair(){
        return this.curNumPairs > this.minPairs;
    }

    /**
     * Check if the leaf node can be merged with a sibling
     * @return - boolean value
     */
    public boolean canMerge(){
        return this.curNumPairs == this.minPairs;
    }
}
