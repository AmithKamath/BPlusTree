import java.util.stream.IntStream;

/**
 * Internal nodes or non-leaf nodes of a B+ tree
 */
public class IndexNode extends Node{
    private IndexNode leftSibling;
    private IndexNode rightSibling;
    private int currentDegree;
    private int maxDegree;
    private int minDegree;
    private Integer[] keys;
    private Node[] children;

    public IndexNode(int m, Integer[] keys){
        this.keys = keys;
        this.currentDegree = 0;
        this.maxDegree = m;
        this.minDegree = (int)Math.ceil(m / 2.0);
        this.children = new Node[m + 1]; //When an index node is overfull, it will have m + 1 children
    }

    public IndexNode(int m, Integer[] keys, Node[] children){
        this.keys = keys;
        for(int i = 0; i < children.length; i++){
            if(children[i] == null){
                this.currentDegree = i;
                break;
            }
        }
        this.maxDegree = m;
        this.minDegree = (int)Math.ceil(m / 2.0);
        this.children = children;
    }

    /**
     * Adds a new child to the end of children array
     * @param node
     */
    public void addChild(Node node){
        this.children[this.currentDegree++] = node;
    }

    /**
     * Returns the index of a child node
     * @param node - the node for which the index has to be found
     * @return - integer index value
     */
    public int getChildIdx(Node node){
        return IntStream.range(0, children.length).filter(i -> this.children[i] == node).findFirst().orElse(-1);
    }

    /**
     * Inserts a child to the children array at a specified index
     * @param node - the new child to be added
     * @param idx - the index at which the new child needs to be added
     */
    public void insertChild(Node node, int idx){
        //shift the child references to the right by 1 index
        for(int i = this.currentDegree - 1; i >= idx; i--){
            this.children[i + 1] = this.children[i];
        }
        this.children[idx] = node;
        this.currentDegree++;
    }

    /**
     * Check if the index node is overfull i.e the current degree exceeds the maximum degree
     * @return - boolean value
     */
    public boolean isExceedingLimit(){
        return this.currentDegree > this.maxDegree;
    }

    /**
     * Removes the child from children array by setting it to null
     * @param idx - index of the child to be removed
     */
    public void removeChild(int idx){
        this.children[idx] = null;
        this.currentDegree--;
    }

    /**
     * Checks if the index node can be merged with it's sibling
     * @return - boolean value
     */
    public boolean canMerge(){
        return this.currentDegree == this.minDegree;
    }

    /**
     * Deletes a key specified at an index and shifts the remaining keys to left by 1
     * @param index - the index of the key to be deleted
     */
    public void deleteKey(int index){
        int i = 0;
        for(i = index; i < this.currentDegree - 2 ; i++){ //Number of keys are always 1 less than the current degree
            this.keys[i] = this.keys[i + 1];
        }
        this.keys[i] = null;
    }

    /**
     * Deletes a child reference from the children array and shifts remaining references to the left by 1
     * @param index - index of the child in children array that is to be deleted
     */
    public void deleteChildReference(int index){
        int i = 0;
        for(i = index; i < this.currentDegree - 1; i++){
            this.children[i] = this.children[i + 1];
        }
        this.children[i] = null;
        this.currentDegree--;
    }

    /**
     * Check if an index node is deficient i.e has a degree less than minimum
     * @return - boolean value
     */
    public boolean isDeficient(){
        return this.currentDegree < this.minDegree;
    }

    /**
     * Check if an index node can lean a key to it's sibling
     * @return - boolean value
     */
    public boolean canLend(){
        return this.currentDegree > this.minDegree;
    }

    /**
     * getter for leftSibling variable
     * @return - IndexNode reference
     */
    public IndexNode getLeftSibling() {
        return leftSibling;
    }

    /**
     * setter for leftSibling variable
     * @param leftSibling - IndexNode reference
     */
    public void setLeftSibling(IndexNode leftSibling) {
        this.leftSibling = leftSibling;
    }

    /**
     * getter for rightSibling variable
     * @return - IndexNode reference
     */
    public IndexNode getRightSibling() {
        return rightSibling;
    }

    /**
     * setter for rightSibling variable
     * @param rightSibling - IndexNode reference
     */
    public void setRightSibling(IndexNode rightSibling) {
        this.rightSibling = rightSibling;
    }

    /**
     * getter for the currentDegree variable
     * @return - int value of the currentDegree
     */
    public int getCurrentDegree() {
        return currentDegree;
    }

    /**
     * setter for the currentDegree variable
     * @param currentDegree - int value for the currentDegree
     */
    public void setCurrentDegree(int currentDegree) {
        this.currentDegree = currentDegree;
    }

    /**
     * getter for keys variable
     * @return - Integer[] reference
     */
    public Integer[] getKeys() {
        return keys;
    }

    /**
     * setter for keys variable
     * @param keys - Integer[] reference
     */
    public void setKeys(Integer[] keys) {
        this.keys = keys;
    }

    /**
     * getter for children variable
     * @return - Node[] reference
     */
    public Node[] getChildren() {
        return children;
    }
}
