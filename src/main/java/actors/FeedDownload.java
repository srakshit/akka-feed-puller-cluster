package actors;

        import model.feed.Feed;
        import org.apache.commons.io.FileUtils;

        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.net.URL;
        import java.net.URLConnection;
        import java.nio.channels.Channels;
        import java.nio.channels.ReadableByteChannel;
        import java.nio.file.Files;
        import java.nio.file.Path;
        import java.nio.file.Paths;
        import java.util.UUID;

/**
 * Created by rakshit on 19/03/2018.
 */
public class FeedDownload {

//    public static void download(Feed feed) throws IOException {
//        Path pathToFile = Paths.get("C:\\Development\\Git\\akka\\akka-feed-puller-cluster\\download\\"+feed.getCompany()+"_"+feed.getFeedName()+"_"+ UUID.randomUUID());
//        Files.createDirectories(pathToFile.getParent());
//        ReadableByteChannel rbc = null;
//        FileOutputStream fos = null;
//
//        try {
//            URL url = new URL(feed.getUrl());
//            URLConnection connection = url.openConnection();
//            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
//            connection.connect();
//            rbc = Channels.newChannel(connection.getInputStream());
//            fos = new FileOutputStream(pathToFile.toString());
//        } catch(IOException e) {
//            throw e;
//        } finally {
//            if (fos != null)
//                fos.close();
//            if (rbc != null)
//                rbc.close();
//        }
//    }

    public static void download(Feed feed) throws IOException {
        File file = new File("downloaded_files/" + feed.getCompany() + "_" + feed.getFeedName() + "_" + UUID.randomUUID());
        URL url=new URL(feed.getUrl());
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
        conn.connect();
        FileUtils.copyInputStreamToFile(conn.getInputStream(), file);
    }
}
