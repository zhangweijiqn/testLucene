import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class TestLucene {
    // 保存路径
    private static String INDEX_DIR = "target/indexed2";
    private static Analyzer analyzer = null;
    private static Directory directory = null;
    private static IndexWriter indexWriter = null;

    public static void main(String[] args) {
        try {
//            index();
            search("Solr");//可以使用 + -符号来匹配要筛选/排除的word：search("man woman -hello");
//            insert();
//            delete("text5");
//            update();
//            indexDocs("src/main/resources/docs/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 更新索引
     *
     * @throws Exception
     */
    public static void update() throws Exception {
        String text1 = "update,hello,man!";
        Date date1 = new Date();
        analyzer = new StandardAnalyzer();
        directory = FSDirectory.open(Paths.get(INDEX_DIR));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, config);

        Document doc1 = new Document();
        doc1.add(new TextField("filename", "text1", Store.YES));
        doc1.add(new TextField("content", text1, Store.YES));

        indexWriter.updateDocument(new Term("filename","text1"), doc1);

        indexWriter.close();

        Date date2 = new Date();
        System.out.println("更新索引耗时：" + (date2.getTime() - date1.getTime()) + "ms\n");
    }
    /**
     * 删除索引
     *
     * @param str 删除的关键字
     * @throws Exception
     */
    public static void delete(String str) throws Exception {
        Date date1 = new Date();
        analyzer = new StandardAnalyzer();
        directory = FSDirectory.open(Paths.get(INDEX_DIR));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, config);

        indexWriter.deleteDocuments(new Term("filename",str));

        indexWriter.close();

        Date date2 = new Date();
        System.out.println("删除索引耗时：" + (date2.getTime() - date1.getTime()) + "ms\n");
    }
    /**
     * 增加索引
     *
     * @throws Exception
     */
    public static void insert() throws Exception {
        String text5 = "hello,goodbye,man,woman";
        Date date1 = new Date();
        analyzer = new StandardAnalyzer();
        directory = FSDirectory.open(Paths.get(INDEX_DIR));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, config);

        Document doc1 = new Document();
        doc1.add(new TextField("filename", "text5", Store.YES));
        doc1.add(new TextField("content", text5, Store.YES));
        indexWriter.addDocument(doc1);

        indexWriter.commit();
        indexWriter.close();

        Date date2 = new Date();
        System.out.println("增加索引耗时：" + (date2.getTime() - date1.getTime()) + "ms\n");
    }
    /**
     * 建立索引
     *  文本
     */
    public static void index() throws Exception {

        String text1 = "hello,man!";
        String text2 = "goodbye,man!";
        String text3 = "hello,woman!";
        String text4 = "goodbye,woman!";

        Date date1 = new Date();
        analyzer = new StandardAnalyzer();
        directory = FSDirectory.open(Paths.get(INDEX_DIR));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);  //设置模式为创建新的索引，会覆盖之前的索引
        indexWriter = new IndexWriter(directory, config);

        Document doc1 = new Document();
        doc1.add(new TextField("filename", "text1", Store.YES));
        doc1.add(new TextField("content", text1, Store.YES));
        indexWriter.addDocument(doc1);

        Document doc2 = new Document();
        doc2.add(new TextField("filename", "text2", Store.YES));
        doc2.add(new TextField("content", text2, Store.YES));
        indexWriter.addDocument(doc2);

        Document doc3 = new Document();
        doc3.add(new TextField("filename", "text3", Store.YES));
        doc3.add(new TextField("content", text3, Store.YES));
        indexWriter.addDocument(doc3);

        Document doc4 = new Document();
        doc4.add(new TextField("filename", "text4", Store.YES));
        doc4.add(new TextField("content", text4, Store.YES));
        indexWriter.addDocument(doc4);

        indexWriter.commit();
        indexWriter.close();

        Date date2 = new Date();
        System.out.println("创建索引耗时：" + (date2.getTime() - date1.getTime()) + "ms\n");
    }

    /**
     * 建立索引
     *  文件
     */
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            // make a new, empty document
            //Document类似数据库中一条记录，可以由不同的Field组成，并且字段可以套用不同的类型。
            Document doc = new Document();

            // Add the path of the file as a field named "path".  Use a
            // field that is indexed (i.e. searchable), but don't tokenize
            // the field into separate words and don't index term frequency
            // or positional information:
            //Field代表与文档相关的元数据
            Field pathField = new StringField("filename", file.toString(), Field.Store.YES);
            doc.add(pathField);

            // Add the last modified date of the file a field named "modified".
            // Use a LongPoint that is indexed (i.e. efficiently filterable with
            // PointRangeQuery).  This indexes to milli-second resolution, which
            // is often too fine.  You could instead create a number based on
            // year/month/day/hour/minutes/seconds, down the resolution you require.
            // For example the long value 2011021714 would mean
            // February 17, 2011, 2-3 PM.
            doc.add(new LongPoint("modified", lastModified));

            // Add the contents of the file to a field named "contents".  Specify a Reader,
            // so that the text of the file is tokenized and indexed, but not stored.
            // Note that FileReader expects the file to be in UTF-8 encoding.
            // If that's not the case searching for special characters will fail.
            doc.add(new TextField("content", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));//文件读入的内容默认调用的模式为TYPE_NOT_STORED

            if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                // New index, so we just add the document (no old document can be there):
                System.out.println("adding " + file);
                writer.addDocument(doc);        //writer.addDocument/updateDocument会执行具体分词建索引的过程。

            } else {
                // Existing index (an old copy of this document may have been indexed) so
                // we use updateDocument instead to replace the old one matching the exact
                // path, if present:
                System.out.println("updating " + file);
                writer.updateDocument(new Term("path", file.toString()), doc);
            }
        }
    }

    /**
     *
     * 建立索引
     * 目录
     *
     */
    static void indexDocs(String docsPath) throws IOException {
        analyzer = new StandardAnalyzer();
        directory = FSDirectory.open(Paths.get(INDEX_DIR));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);//设置索引是创建新的还是添加到已有的上面(/OpenMode.CREATE)
        indexWriter = new IndexWriter(directory, config);
        Path path = Paths.get(docsPath);
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(indexWriter, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(indexWriter, path, Files.getLastModifiedTime(path).toMillis());
        }
        indexWriter.commit();
        indexWriter.close();
    }

    /**
     * 关键字查询
     *
     * @param str
     * @throws Exception
     */
    public static void search(String str) throws Exception {
        analyzer = new StandardAnalyzer();
        directory = FSDirectory.open(Paths.get(INDEX_DIR));

        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);

        QueryParser parser = new QueryParser("content",analyzer);  //检索content字段的值
        Query query = parser.parse(str);

        ScoreDoc[] hits = isearcher.search(query, 100).scoreDocs;
        //这里可以加入分页显示实现
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            System.out.println((i+1)+","+hitDoc.get("filename")+",score="+hits[i].score);
            System.out.println(hitDoc.get("content"));//文件建立的索引默认获取不到content
        }
        ireader.close();
        directory.close();
    }
}