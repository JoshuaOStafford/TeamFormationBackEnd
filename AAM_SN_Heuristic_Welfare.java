/**
 * Created by loujian on 11/6/16.
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.lang.*;
import org.apache.commons.math3.*;


public class AAM_SN_Heuristic_Welfare {

    public static void main(String[] args)throws Exception
    {
        long startTime = System.currentTimeMillis();

        Scanner cin=new Scanner(new File("SN_Scale_Free_1000_n10m2.txt"));//number of neighbors, then the list of preference for neighbors
        File writename = new File("AAM_heuristic_SN_Scale_Free_1000_n10m2.txt");
        writename.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(writename));

        int n = 10; //n means the number of players in the game.

        int num_cases=1000; //here is the number of test case


        double alpha=0.0000001; //here is parameter for the heuristic of RPM
        double lower=alpha;
        double upper=1.0-alpha;


        double[][]rank= new double[n+1][num_cases+1]; //Here is the rank of players in all test cases.

        for(int iter=1; iter<=num_cases; iter++) {

            int depth = n;// depth means search depth

            LinkedList<Integer> order = new LinkedList<Integer>(); //means the list of players in the order
            order.clear();



            boolean[] flag_order= new boolean[n+1];
            for(int i=1; i<=n; i++)
                flag_order[i]=false;

            Random rd= new Random();

            while(order.size()<n)
            {
                Integer tmp= rd.nextInt(n);
                tmp++;
                if(flag_order[tmp]==false) {
                    order.add(tmp);
                    flag_order[tmp]=true;
                }
                else
                {
                    continue;
                }
            }

            for(int i=0; i<order.size(); i++)
                rank[order.get(i)][iter]=(double)(i+1)/n; //get the rank of each player we can get a matrix, rank[i][j] means the utility of player i's utility in the case iter


            ArrayList<ArrayList<Integer>> value = new ArrayList<ArrayList<Integer>>(); //simulate the preference profile of all players
            ArrayList<LinkedList<Integer>> linked_value = new ArrayList<LinkedList<Integer>>();
            ArrayList<LinkedList<Integer>> linked_value1 = new ArrayList<LinkedList<Integer>>();
            ArrayList<LinkedList<Integer>> linked_value_copy = new ArrayList<LinkedList<Integer>>();

            double[][] normal_value = new double[n+1][n+1]; //here means the normalized value each player get
            for(int i=1; i<=n; i++)
                for(int j=1; j<=n; j++)
                    normal_value[i][j]=0;

            value.clear();
            linked_value.clear();
            linked_value1.clear();
            linked_value_copy.clear();
            for (int i = 0; i <= n; i++) {
                ArrayList<Integer> tmp = new ArrayList<Integer>();
                LinkedList<Integer> tmp1 = new LinkedList<Integer>();
                LinkedList<Integer> tmp2 = new LinkedList<Integer>();
                LinkedList<Integer> tmp3 = new LinkedList<Integer>();
                tmp.clear();
                tmp1.clear();
                tmp2.clear();
                tmp3.clear();
                for (int j = 0; j <= n; j++) {
                    if(j==i)
                        tmp.add(0);
                    else
                        tmp.add(-1);
                }
                value.add(tmp);
                linked_value.add(tmp1);
                linked_value1.add(tmp2);
                linked_value_copy.add(tmp3);
            }

            for (int i = 1; i <= n; i++)
            {
                Integer number_nei= cin.nextInt();
                for (int j = 1; j <= number_nei; j++) {
                    Integer tmp = cin.nextInt();
                    out.write(tmp+" ");
                    value.get(i).set(tmp, n - j + 1); //here we let it be the linear decreasing function

                    normal_value[i][tmp]= 2.0*(number_nei-j+1)/number_nei -1 ; //here we normalize the utility of players
                    if(normal_value[i][tmp]>0 ) {
                        //value.get(i).set(tmp, 1<<(n - j)); //here we let it be the exponential decreasing function
                        linked_value.get(i).add(tmp);
                        linked_value1.get(i).add(tmp);
                        linked_value_copy.get(i).add(tmp);
                    }
                }

                out.write("\r\n");
            }



            AAM_SN_Heuristic TF_H = new AAM_SN_Heuristic(n, depth, order, value, linked_value, lower, upper);
            ArrayList<Integer> array = TF_H.ARM();





            Integer[] teammate= new Integer[n+1];

            for(int i=1; i<=n; i++)
                teammate[i]=0; //here teammate[i]=0 means no teammmate


            //Here is the outcome of a match, you can feel free to revise it
            for (int i = 1; i <= n; i++) {
                if (array.get(i) == 0) {
                    out.write(i + "\r\n");
                } else {
                    teammate[i] = value.get(i).indexOf(array.get(i));
                    out.write(i + " with " + teammate[i] + "\r\n");

                }
            }
        }
        out.flush();
        out.close();

    }
}
