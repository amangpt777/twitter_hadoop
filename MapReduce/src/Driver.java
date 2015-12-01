import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class Driver {	
	public static void main(String[] args) throws Exception {
		boolean allPathFound;
		boolean allCommunityFound;
		boolean isSelected;
		
		Path inputPath = new Path(args[0]);
		Path outputPath0 = new Path(args[1] + "/output0");
		job0(inputPath, outputPath0);
		
		Path outputPath1 = new Path(args[1] + "/output1");
		int outer = 1;
		while (true) {
			int inner = 1;
			allPathFound = false;
			while (!allPathFound) {
				allPathFound = job1(outputPath0, outputPath1);
				Path temp = outputPath0;
				outputPath0 = outputPath1;
				outputPath1 = temp;
				
				System.out.println("finding shortest path: iteration #" + String.valueOf(inner));
				System.out.println(allPathFound);
				inner++;
			}
			
			Path outputPath2 = new Path(args[1] + "/output2");
			job2(outputPath0, outputPath2);
			
			Path outputPath3 = new Path(args[1] + "/output3");
			isSelected = job3(outputPath2, outputPath3);
			
			System.out.println("removing edges: iteration #" + String.valueOf(outer));
			System.out.println(isSelected);
			outer++;
			
			if (!isSelected)
				break;
			
			Path outputPath4 = new Path(args[1] + "/output4");
			job4(outputPath0, outputPath4);
			Path temp = outputPath0;
			outputPath0 = outputPath4;
			outputPath4 = temp;
		}
		
		// job6 is to generate output for visualization (after detecting community)
		Path adjListPath = new Path("./result");
		Path outputPath5 = new Path(args[1] + "/output5");
		job5(adjListPath, outputPath5);
		
		// start to group users into community
		Path outputPath6 = new Path(args[1] + "/output6");
		job6(adjListPath, outputPath6);
		
		Path outputPath7 = new Path(args[1] + "/output7");
		Path outputPath8 = new Path(args[1] + "/output8");
		allCommunityFound = false;
		while (!allCommunityFound) {
			allCommunityFound = job7(outputPath6, outputPath7);
			job8(outputPath6, outputPath8);
			Path temp = outputPath6;
			outputPath6 = outputPath8;
			outputPath8 = temp;
		}
		
		Path outputPath9 = new Path(args[1] + "/output9");
		job9(outputPath6, outputPath9);
	}
	
	private static void job0(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "generating input format");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper0.class);
		job.setReducerClass(Reducer0.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		Path p = new Path("./result/adjList");
		fs.delete(new Path("./result/adjList"));
		fs.createNewFile(p);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
	}
	
	private static boolean job1(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "finding shortest paths");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper1.class);
		job.setReducerClass(Reducer1.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
		Path p = new Path("./notAllPathFound");
		boolean allPathFound = !fs.exists(p);
		fs.delete(p);
		return allPathFound;
	}
	
	private static void job2(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "calculating edge betweenness");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper2.class);
		job.setReducerClass(Reducer2.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(DoubleWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
	}
	
	private static boolean job3(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "selecting edges to be removed");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper3.class);
		job.setReducerClass(Reducer3.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		Path p = new Path("./selectedEdges");
		fs.createNewFile(p);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
		boolean isSelected = fs.exists(p);
		return isSelected;
	}
	
	private static void job4(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "removing edges selected");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper4.class);
		job.setReducerClass(Reducer4.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
		
		HashMap<String, Set<String>> map = new HashMap<>();
		try {
			Path p = new Path("./selectedEdges");
			FSDataInputStream in = fs.open(p);
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line;
			
			while ((line = br.readLine()) != null) {
				String[] s = line.split(",");
				if (!map.containsKey(s[0]))
					map.put(s[0], new HashSet<String>());
				map.get(s[0]).add(s[1]);
			}
			br.close();
			fs.delete(p);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			Path p = new Path("./result/adjList");
			Path newp = new Path("./result/adjListUpdated");
			fs.createNewFile(newp);
			FSDataInputStream in = fs.open(p);
			FSDataOutputStream out = fs.append(newp);
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
			String line;			
			while ((line = br.readLine()) != null) {
				String[] users = line.split(" |,");
				if (!map.containsKey(users[0])) {
					bw.write(line);
					bw.write("\n");
				} else {
					bw.write(users[0]);
					for (int i = 1; i < users.length; i++) {
						if (!map.get(users[0]).contains(users[i]))
							bw.write(" " + users[i]);
					}
					bw.write("\n");
				}
			}
			br.close();
			bw.close();
			fs.delete(p);
			fs.rename(newp, p);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private static void job5(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "generating output for visualization");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper5.class);
		//job.setReducerClass(Reducer6.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		//job.setOutputKeyClass(Text.class);
		//job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
	}
	
	private static void job6(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "adding communityNum");
		conf.setLong("communityNum", 0);
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper6.class);
		job.setReducerClass(Reducer6.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
	}
	
	private static boolean job7(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "selecting the smallest communityNum");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper7.class);
		job.setReducerClass(Reducer7.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
		Path p = new Path("./communityNum");
		boolean allCommunityFound = !fs.exists(p);
		return allCommunityFound;
	}
	
	private static void job8(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "updating communityNum");
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper8.class);
		job.setReducerClass(Reducer8.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
		Path p = new Path("./communityNum");
		fs.delete(p);
	}
	
	private static void job9(Path inputPath, Path outputPath) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "adding communityNum");
		conf.setLong("communityNum", 0);
		job.setJarByClass(Driver.class);
		job.setMapperClass(Mapper9.class);
		job.setReducerClass(Reducer9.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileSystem fs = FileSystem.get(new URI(outputPath.toString()), conf);
		fs.delete(outputPath);
		FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		System.out.println(job.waitForCompletion(true) ? "Success" : "Fail");
	}
}
