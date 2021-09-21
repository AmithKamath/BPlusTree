import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class that represents a B+ tree
 */
public class bplustree {
    private final int m;
    private IndexNode root;
    private LeafNode leftMostLeaf; //A reference to the leftmost leaf node

    public bplustree(int m){
        this.m = m;
        this.root = null;
    }

    /**
     * Below are the different operations that an input file can have
     */
    static final String INSERT = "Insert";
    static final String INITIALIZE = "Initialize";
    static final String DELETE = "Delete";
    static final String SEARCH = "Search";
    static final String NULL = "Null";

    /**
     * Returns the leaf node for a given key
     * @param node - Index node from which the search should start
     * @param key - key to be searched
     * @return - LeafNode that the key belongs to
     */
    private LeafNode getLeafNode(IndexNode node, int key){
        //The keys are k0, k1, k2, k3..
        //the child references are c0,c1,c2,c3.. where c0 < k0 <= c1 < k1 <= c2
        //Hence, the index i of first key that is greater than target key is returned. The corresponding child at that
        //index will be pointer to a subtree that has value less than the key at index i
        int idx = getChildSubtreeIdxForAKey(node, key);
        Node child = node.getChildren()[idx];
        if(child instanceof IndexNode){
            return getLeafNode((IndexNode) child, key);
        }else{
            return (LeafNode) child;
        }
    }

    /**
     * Returns the index of child subtree to which the key belongs to
     * @param node - IndexNode under consideration for the search
     * @param key - value for the search
     * @return - index of the child subtree to which the key belongs to
     */
    private int getChildSubtreeIdxForAKey(IndexNode node, int key){
        //Binary search
        Integer[] keys = node.getKeys();

        //r starts from currentDegree - 2 since r is 0-index based and number of keys = currentDegree - 1
        int l = 0, r = node.getCurrentDegree() - 2;
        while(l <= r){
            int mid = l + (r - l) / 2;
            if(keys[mid] <= key){
                l = mid + 1;
            }else{
                r = mid - 1;
            }
        }
        return l;
    }

    /**
     * Split pairs by copying half of the pairs in the original array to a new array
     * @param leafNode - LeafNode whose pairs have to be split
     * @param splitIdx- the index for split
     * @return - an array of Pair that has the right half of the original pairs
     */
    private Pair[] splitPairs(LeafNode leafNode, int splitIdx){
        Pair[] pairs = leafNode.getPairs() ;
        Pair[] newLeafNodePairs = new Pair[this.m];
        int k = 0;
        for(int i = splitIdx; i < pairs.length; i++){
            newLeafNodePairs[k++] = pairs[i];
            leafNode.deletePair(i);
        }
        return newLeafNodePairs;
    }

    /**
     * Split keys array at the splitIdx
     * @param keys - Integer keys array to be split
     * @param splitIdx - Index at which the keys array have to be split
     * @return - new Integer array that contains all the key from splitIdx + 1 of the old keys array
     */
    private Integer[] splitKeys(Integer[] keys, int splitIdx){
        Integer[] splitKeys = new Integer[this.m];
        int k = 0;
        keys[splitIdx] = null;

        for(int i = splitIdx + 1; i < keys.length; i++){
            splitKeys[k++] = keys[i];
            keys[i] = null;
        }
        return splitKeys;
    }

    /**
     * Split children array at the splitIdx
     * @param node - Index node whose children have to be split
     * @param splitIdx - Index at which the children array have to be split
     * @return - new Node array that contains all the children from splitIdx + 1 of the old children array
     */
    private Node[] splitChildren(IndexNode node, int splitIdx){
        Node[] splitChildren = new Node[this.m + 1];
        int k = 0;
        Node[] children = node.getChildren();
        for(int i = splitIdx + 1; i < children.length; i++){
            splitChildren[k++] = children[i];
            node.removeChild(i);
        }
        return splitChildren;
    }

    /**
     * Split the index node if it is overfull
     * @param indexNode - reference to the index node
     */
    private void splitIndexNodeIfOverfull(IndexNode indexNode){
        if(indexNode == null){
            return;
        }

        if(!indexNode.isExceedingLimit()){
            return;
        }

        //The current index node is split by creating a new index node with half of the keys and child from the
        //old node.

        int midIdx = (int) Math.ceil((this.m + 1) / 2.0) - 1;
        int newKey = indexNode.getKeys()[midIdx];
        Integer[] splitKeys = splitKeys(indexNode.getKeys(), midIdx);
        Node[] splitChildren = splitChildren(indexNode, midIdx);

        //calculate new degree of the current index node
        Node[] children = indexNode.getChildren();
        for(int i = 0; i < children.length; i++){
            if(children[i] == null){
                indexNode.setCurrentDegree(i);
                break;
            }
        }

        //Create a new index node with split keys and children
        IndexNode newSibling = new IndexNode(this.m, splitKeys, splitChildren);
        Arrays.stream(splitChildren).forEach((child) -> {
            if(child != null){
                child.parent = newSibling;
            }
        });

        //update sibling pointers
        newSibling.setRightSibling(indexNode.getRightSibling());
        if(newSibling.getRightSibling() != null){
            newSibling.getRightSibling().setLeftSibling(newSibling);
        }
        indexNode.setRightSibling(newSibling);
        newSibling.setLeftSibling(indexNode);

        //The key at the split index i.e midIdx is added to the parent and a new child pointer reference to the new
        //index node is added in the parent
        IndexNode parent = indexNode.parent;
        if(parent != null){
            parent.getKeys()[parent.getCurrentDegree() - 1] = newKey;
            Arrays.sort(parent.getKeys(), 0, parent.getCurrentDegree());
            int newSiblingIdx = parent.getChildIdx(indexNode) + 1;
            parent.insertChild(newSibling, newSiblingIdx);
            newSibling.parent = parent;
        }else{
            //A new index node is created and is set as the root node
            Integer[] keys = new Integer[this.m];
            keys[0] = newKey;
            IndexNode newRoot = new IndexNode(this.m, keys);
            newRoot.addChild(indexNode);
            newRoot.addChild(newSibling);
            indexNode.parent = newRoot;
            newSibling.parent = newRoot;
            this.root = newRoot;
        }

        //recursive call for the parent
        splitIndexNodeIfOverfull(indexNode.parent);
    }

    /**
     * split the leaf node
     * @param leafNode - the leaf node that is to be split
     */
    private void splitLeafNode(LeafNode leafNode){
        int midIdx = (int)Math.ceil((this.m + 1) / 2.0) - 1;
        Pair[] newLeafNodePairs = splitPairs(leafNode, midIdx);

        if(leafNode.parent == null){
            //If the parent is null, a new index node is created. The first key of this index node is equal
            //to the first key of it's new child which is not added yet
            Integer[] keys = new Integer[this.m];
            keys[0] = newLeafNodePairs[0].key;
            IndexNode parent = new IndexNode(this.m, keys);
            leafNode.parent = parent;
            parent.addChild(leafNode);
        }else{
            //parent is not null and hence, a new key is inserted to the parent and the parent keys are sorted
            int newKey = newLeafNodePairs[0].key;
            leafNode.parent.getKeys()[leafNode.parent.getCurrentDegree() - 1] = newKey;
            Arrays.sort(leafNode.parent.getKeys(), 0, leafNode.parent.getCurrentDegree());
        }

        //Create a new leaf node with newLeafNodePairs created earlier
        LeafNode newSibling = new LeafNode(this.m, newLeafNodePairs, leafNode.parent);

        //Identify the index of the current leafNode in the parent's children reference array.
        //The index is incremented by 1 since the new leaf node will be a right sibling of the current leaf node
        int newSiblingIdx = leafNode.parent.getChildIdx(leafNode) + 1;
        leafNode.parent.insertChild(newSibling, newSiblingIdx);

        //Modify the sibling references to incorporate the newly added leaf node
        newSibling.setRightSibling(leafNode.getRightSibling());
        if(newSibling.getRightSibling() != null){
            newSibling.getRightSibling().setLeftSibling(newSibling);
        }
        leafNode.setRightSibling(newSibling);
        newSibling.setLeftSibling(leafNode);

        if(this.root != null){
            splitIndexNodeIfOverfull(leafNode.parent);
        }else{
            this.root = leafNode.parent;
        }
    }

    /**
     * Inserts a new key value pair into the B+ tree. If the inserted node causes a leaf node to be overfull,
     * a new leaf is created with half of the values from the overfull node and a new key is added to parent node along
     * with a child reference to the new node. If the parent node is overful,it is further split and the process
     * continues untill there are no overfull nodes in the tree
     * @param key - key of the dictionary pair to be inserted
     * @param value - value of the dictionary pair to be inserted
     */
    public void insert(int key, double value){
        Pair newPair = new Pair(key, value);

        if(this.leftMostLeaf == null){ //There are no nodes in the tree
            this.leftMostLeaf = new LeafNode(this.m, newPair);
            return;
        }

        LeafNode leafNode = this.root == null? this.leftMostLeaf : getLeafNode(this.root, key);

        if(!leafNode.addPair(newPair)){
            //leaf node is full
            int curNumPairs = leafNode.getCurNumPairs();
            leafNode.getPairs()[curNumPairs] = newPair;
            leafNode.setCurNumPairs(curNumPairs + 1);
            leafNode.sortPairs();
            //leaf node is overfull and needs to be split
            splitLeafNode(leafNode);
        }

    }

    /**
     * Search for a given key in the B+ tree
     * @param key - the key to be searched
     * @return - Double value associated with the key. If the key is not found, then the value is null
     */
    public Double search(int key){
        if(this.leftMostLeaf == null){
            return null;
        }
        LeafNode node = this.root == null ? this.leftMostLeaf : getLeafNode(this.root, key);
        int idx = node.search(key);
        return idx == -1 ? null : node.getPairs()[idx].value;
    }

    /**
     * Search for a range of values in the B+ tree
     * @param key1 - the lower bound of the search
     * @param key2 -  the higher bound of the search
     * @return - List<Double> contains values of all the keys that fall in the range
     */
    public List<Double> search(int key1, int key2){
        List<Double> result = new ArrayList<>();

        if(this.leftMostLeaf == null){
            return result;
        }

        LeafNode node = this.root == null ? this.leftMostLeaf : getLeafNode(this.root, key1);
        boolean stopLoop = false;

        //Once a leaf node is found, keep traversing through it's right sibling until a key > high is found
        while(node != null){
            for(Pair pair : node.getPairs()){
                if(pair == null){
                    break;
                }
                if(pair.key >= key1 && pair.key <= key2){
                    result.add(pair.value);
                }else if(pair.key > key2){
                    stopLoop = true;
                    break;
                }
            }
            if(stopLoop){
                break;
            }
            node = node.getRightSibling();
        }

        return result;
    }

    /**
     * checks if a leafNode can borrow a pair from it's right sibling
     * @param leafNode - reference to a leaf node
     * @return - boolean value
     */
    private boolean canBorrowFromRightSibling(LeafNode leafNode){
        LeafNode rightSibling = leafNode.getRightSibling();
        return rightSibling != null && rightSibling.parent == leafNode.parent && rightSibling.canLendAPair();

    }

    /**
     * checks if an IndexNode can borrow a key from it's right sibling
     * @param indexNode - reference to an index node
     * @return - boolean value
     */
    private boolean canBorrowFromRightSibling(IndexNode indexNode){
        IndexNode rightSibling = indexNode.getRightSibling();
        return rightSibling != null && rightSibling.parent == indexNode.parent && rightSibling.canLend();
    }

    /**
     * checks if a leafNode can borrow a pair from it's left sibling
     * @param leafNode - reference to a leaf node
     * @return - boolean value
     */
    private boolean canBorrowFromLeftSibling(LeafNode leafNode){
        LeafNode leftSibling = leafNode.getLeftSibling();
        return leftSibling != null && leftSibling.parent == leafNode.parent && leftSibling.canLendAPair();
    }

    /**
     * checks if an IndexNode can borrow a pair from it's left sibling
     * @param indexNode - reference to an index node
     * @return - boolean value
     */
    private boolean canBorrowFromLeftSibling(IndexNode indexNode){
        IndexNode leftSibling = indexNode.getLeftSibling();
        return leftSibling != null && leftSibling.parent == indexNode.parent && leftSibling.canLend();
    }

    /**
     * checks if a leafNode can merge with it's right sibling
     * @param leafNode - reference to a leaf node
     * @return - boolean value
     */
    private boolean canMergeWithRightSibling(LeafNode leafNode){
        LeafNode rightSibling = leafNode.getRightSibling();
        return rightSibling != null && rightSibling.parent == leafNode.parent && rightSibling.canMerge();
    }

    /**
     * checks if an IndexNode can merge with it's right sibling
     * @param indexNode - reference to an index node
     * @return - boolean value
     */
    private boolean canMergeWithRightSibling(IndexNode indexNode){
        IndexNode rightSibling = indexNode.getRightSibling();
        return rightSibling != null && rightSibling.parent == indexNode.parent && rightSibling.canMerge();
    }

    /**
     * checks if a leafNode can merge with it's left sibling
     * @param leafNode - reference to a leaf node
     * @return - boolean value
     */
    private boolean canMergeWithLeftSibling(LeafNode leafNode){
        LeafNode leftSibling = leafNode.getLeftSibling();
        return leftSibling != null && leftSibling.parent == leafNode.parent && leftSibling.canMerge();
    }

    /**
     * checks if an indexNode can merge with it's left sibling
     * @param indexNode - reference to an index node
     * @return - boolean value
     */
    private boolean canMergeWithLeftSibling(IndexNode indexNode){
        IndexNode leftSibling = indexNode.getLeftSibling();
        return leftSibling != null && leftSibling.parent == indexNode.parent && leftSibling.canMerge();
    }

    /**
     * fix deficiency in an index node either by borrowing a key from sibling or merging with the sibling
     * @param node - reference to an index node
     */
    public void fixDeficiencyInIndexNode(IndexNode node){
        IndexNode parent = node.parent;
        IndexNode rightSibling = node.getRightSibling();
        IndexNode leftSibling = node.getLeftSibling();

        if(this.root == node){

            //The root should have a minimum degree of 2 and is not affected by minDegree
            if(this.root.getCurrentDegree() > 1){
                return;
            }

            Node[] children = node.getChildren();
            if(children[0] instanceof IndexNode){
                this.root = (IndexNode)children[0];
                this.root.parent = null;
            }else{
                this.root = null;
            }
        }else if(canBorrowFromRightSibling(node)){
            //borrow first key and first child reference of the right sibling
            int borrowedKey = rightSibling.getKeys()[0];
            Node child = rightSibling.getChildren()[0];

            int index = parent.getChildIdx(node);

            node.addChild(child);
            //Add the parent key to index node
            node.getKeys()[node.getCurrentDegree() - 2] = parent.getKeys()[index];
            child.parent = node;

            //update the parent key to the borrowed key from right sibling
            parent.getKeys()[index] = borrowedKey;

            //delete the lended key and child reference in the right sibling
            rightSibling.deleteKey(0);
            rightSibling.deleteChildReference(0);
        }else if(canBorrowFromLeftSibling(node)){
            //borrow last key and child reference of the left sibling
            int borrowedKey = leftSibling.getKeys()[leftSibling.getCurrentDegree() - 2];
            Node child = leftSibling.getChildren()[leftSibling.getCurrentDegree() - 1];

            int index = parent.getChildIdx(node);

            //Add parent key to the 0th index of the index node
            Integer[] nodeKeys = node.getKeys();
            for(int i = node.getCurrentDegree() - 2; i >= 0; i--){
                nodeKeys[i + 1] = nodeKeys[i];
            }
            nodeKeys[0] = parent.getKeys()[index - 1];
            node.insertChild(child, 0);
            child.parent = node;

            //update the parent key to the borrowed key from the left sibling
            parent.getKeys()[index - 1] = borrowedKey;

            //delete the lended key and child from the left sibling
            leftSibling.deleteKey(leftSibling.getCurrentDegree() - 2);
            leftSibling.deleteChildReference(leftSibling.getCurrentDegree() - 1);
        }else if(canMergeWithRightSibling(node)){
            int index = parent.getChildIdx(node);

            //When merging with the right sibling, the new right sibling keys will consist of
            // all remaining keys from index node + parent key + right sibling keys
            Integer[] newKeys = new Integer[this.m];
            Integer[] nodeKeys = node.getKeys();
            Integer[] rightSiblingKeys = rightSibling.getKeys();
            int i;
            for(i = 0; i < node.getCurrentDegree() - 1; i++){
                newKeys[i] = nodeKeys[i];
            }
            newKeys[i++] = parent.getKeys()[index];
            for(int j = 0; j < rightSibling.getCurrentDegree() - 1; j++){
                newKeys[i++] = rightSiblingKeys[j];
            }
            rightSibling.setKeys(newKeys);

            //All the children of indexNode is prepended to the children of right sibling
            Node[] nodeChildren = node.getChildren();
            for (int k = nodeChildren.length - 1; k >= 0; k--) {
                if (nodeChildren[k] != null) {
                    rightSibling.insertChild(nodeChildren[k], 0);
                    nodeChildren[k].parent = rightSibling;
                }
            }

            //The parent key and child reference to the indexNode is deleted
            parent.deleteKey(index);
            parent.deleteChildReference(index);

            //update sibling references
            rightSibling.setLeftSibling(node.getLeftSibling());
            if(rightSibling.getLeftSibling() != null){
                rightSibling.getLeftSibling().setRightSibling(rightSibling);
            }
        }else if(canMergeWithLeftSibling(node)){
            int index = parent.getChildIdx(node);

            //When merging with the left sibling, the new left sibling keys will consist of
            // left sibling keys + parent key + all remaining keys from index node
            Integer[] newKeys = new Integer[this.m];
            Integer[] leftSiblingKeys = leftSibling.getKeys();
            Integer[] nodeKeys = node.getKeys();
            int i = 0;
            for(i = 0; i < leftSibling.getCurrentDegree() - 1; i++){
                newKeys[i] = leftSiblingKeys[i];
            }
            newKeys[i++] = parent.getKeys()[index - 1];
            for(int j = 0; j < node.getCurrentDegree() - 1; j++){
                newKeys[i++] = nodeKeys[j];
            }
            leftSibling.setKeys(newKeys);

            //All the children of indexNode is appended to the children of left sibling
            Node[] nodeChildren = node.getChildren();
            for(int k = 0; k < nodeChildren.length; k++){
                if(nodeChildren[k] != null){
                    leftSibling.addChild(nodeChildren[k]);
                    nodeChildren[k].parent = leftSibling;
                }
            }

            //The parent key and child reference to the indexNode is deleted
            parent.deleteKey(index - 1);
            parent.deleteChildReference(index);

            //update sibling references
            leftSibling.setRightSibling(node.getRightSibling());
            if(leftSibling.getRightSibling() != null){
                leftSibling.getRightSibling().setLeftSibling(leftSibling);
            }
        }

        //since merge operation results in a key and child reference being deleted from parent node, it is possible
        //that a parent node can become deficient. If so, handle that scenario
        if (parent != null && parent.isDeficient()) {
            fixDeficiencyInIndexNode(parent);
        }
    }

    /**
     * Deletes a pair from B+ tree that has key equal to the target key
     * @param key - target key
     */
    public void delete(int key){
        if(this.leftMostLeaf == null){
            return;
        }

        LeafNode leafNode = this.root == null ? this.leftMostLeaf : getLeafNode(this.root, key);
        int idx = leafNode.search(key);

        if(idx == -1){
            return; //the key is not found in the B+ tree
        }

        //delete the pair from leaf node and sort all the pairs
        leafNode.deletePair(idx);
        leafNode.sortPairs();

        if(leafNode.isDeficient()){

            LeafNode rightSibling = leafNode.getRightSibling();
            LeafNode leftSibling = leafNode.getLeftSibling();
            IndexNode parent = leafNode.parent;

            if(canBorrowFromRightSibling(leafNode)){
                //the first pair from right sibling is added to the leaf node
                //the first pair in right sibling is deleted and remaining pairs are sorted
                //parent key is updated if needed
                Pair borrowedPair = rightSibling.getPairs()[0];
                leafNode.addPair(borrowedPair);
                rightSibling.deletePair(0);
                rightSibling.sortPairs();

                //idx is the index of key which is greater than all the values in the left subtree of key
                //i.e parent.children[idx]. Hence, when a pair is borrowed from right sibling, the parent key has to be
                //updated if borrowed pair key is >= parent.keys[idx];
                idx = parent.getChildIdx(leafNode);
                if(borrowedPair.key >= parent.getKeys()[idx]){
                    parent.getKeys()[idx] = rightSibling.getPairs()[0].key;
                }
            }else if(canBorrowFromLeftSibling(leafNode)){
                //the last pair in left sibling is added to the leaf node and pairs are sorted
                //the last pair in left sibling is deleted
                //parent key is updated if needed
                Pair borrowedPair = leftSibling.getPairs()[leftSibling.getCurNumPairs() - 1];
                leafNode.addPair(borrowedPair);
                leafNode.sortPairs();
                leftSibling.deletePair(leftSibling.getCurNumPairs() - 1);

                //idx - 1 is the index of key which is <=  the values in the right subtree of keys[idx - 1]
                //i.e parent.children[idx]. Hence, when a pair is borrowed from left sibling, the parent key has to be
                //updated if borrowed pair key is < parent.keys[idx - 1];
                idx = parent.getChildIdx(leafNode);
                if(borrowedPair.key < parent.getKeys()[idx - 1]){
                    parent.getKeys()[idx - 1] = leafNode.getPairs()[0].key;
                }
            }else if(canMergeWithRightSibling(leafNode)){
                //while merging, the parent key is deleted and the left and right child of the parent are merged.
                //While merging with the right sibling, all the remaining pairs of the leafnode are added to
                //the right sibling

                idx = parent.getChildIdx(leafNode);
                parent.deleteKey(idx);
                parent.deleteChildReference(idx);

                for(int i = 0; i < leafNode.getPairs().length; i++){
                    if(leafNode.getPairs()[i] != null){
                        rightSibling.addPair(leafNode.getPairs()[i]);
                    }
                }

                //update sibling references
                rightSibling.setLeftSibling(leafNode.getLeftSibling());
                if (rightSibling.getLeftSibling() == null) {
                    this.leftMostLeaf = rightSibling;
                }else{
                    rightSibling.getLeftSibling().setRightSibling(rightSibling);
                }

                //since a parent key and child is deleted, the parent degree might be < minimum degree of a node.
                //If that is the case, need to fix the deficiency in the parent
                if (parent.isDeficient()) {
                    fixDeficiencyInIndexNode(parent);
                }
            }else if(canMergeWithLeftSibling(leafNode)){
                //while merging, the parent key is deleted and the left and right child of the parent are merged.
                //While merging with the left sibling, all the remaining pairs of the leafnode are added to
                //the left sibling

                idx = parent.getChildIdx(leafNode);
                parent.deleteKey(idx - 1);
                parent.deleteChildReference(idx);

                for(int i = 0; i < leafNode.getPairs().length; i++){
                    if(leafNode.getPairs()[i] != null){
                        leftSibling.addPair(leafNode.getPairs()[i]);
                    }
                }

                //update sibling references
                leftSibling.setRightSibling(leafNode.getRightSibling());
                if(leftSibling.getRightSibling() != null){
                    leafNode.getRightSibling().setLeftSibling(leftSibling);
                }

                //since a parent key and child is deleted, the parent degree might be < minimum degree of a node.
                //If that is the case, need to fix the deficiency in the parent
                if (parent.isDeficient()) {
                    fixDeficiencyInIndexNode(parent);
                }

            }else if(this.root == null && this.leftMostLeaf.getCurNumPairs() == 0){
                //incase the last remaining pair of a B+ tree is deleted
                this.leftMostLeaf = null;
            }
        }
    }

    /**
     * Main function
     * @param args - arguments passed while invoking this java program
     */
    public static void main(String args[]){
        if(args.length != 1){
            System.out.println("Pass the input file name as an arguement to the java program");
            return;
        }

        //All the outputs are written to a file named output_file.txt
        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("output_file.txt"));
            BufferedReader bufferedReader = new BufferedReader(new FileReader(args[0]+".txt"))){
            String line = bufferedReader.readLine();
            bplustree tree = null;
            while(line != null){
                line = line.trim();
                String[] parts = line.split("[,()]"); //Input will be of the form Insert(1,6.7), Search(1)

                switch(parts[0].trim()){
                    case INITIALIZE:
                        tree = new bplustree(Integer.parseInt(parts[1].trim()));
                        break;
                    case INSERT:
                        tree.insert(Integer.parseInt(parts[1].trim()), Double.parseDouble(parts[2].trim()));
                        break;
                    case SEARCH:
                        String result = "";
                        if(parts.length == 2){
                            Double value = tree.search(Integer.parseInt(parts[1].trim()));
                            result = value == null ? NULL : String.valueOf(value);
                        }else{
                            StringBuffer sb = new StringBuffer();
                            List<Double> list = tree.search(Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
                            if(list.size() == 0){
                                sb.append(NULL);
                            }else{
                                list.forEach((value) -> sb.append(value).append(','));
                                sb.deleteCharAt(sb.length() - 1);
                            }
                            bufferedWriter.write(sb.toString());
                        }
                        bufferedWriter.write(result);
                        bufferedWriter.newLine();
                        break;
                    case DELETE:
                        tree.delete(Integer.parseInt(parts[1].trim()));
                        break;
                    default:
                        System.out.println("Invalid operation "+ parts[0]);
                }

                line = bufferedReader.readLine();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

