package infrastructure;


import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infrastructure.Entity.Fields.Field;

/**
 * This example shows how to read collections and entities as XML or into objects.
 */
public class ReadExample {

   public static void main(String[] args) throws Exception {
        new ReadExample().readExample(
             "http://" + Constants.HOST + ":" +
                           Constants.PORT + "/qcbin",
                        Constants.DOMAIN,
                        Constants.PROJECT,
                        Constants.USERNAME,
                        Constants.PASSWORD);
    }

    public void readExample(final String serverUrl, final String domain,
        final String project, String username, String password)
        throws Exception {
    	
        RestConnector con =
                RestConnector.getInstance().init(
                        new HashMap<String, String>(),
                        serverUrl,
                        domain,
                        project);

        // Use the login example code to login for this test.
        // Go over this code to learn how to authenticate/login/logout
        QC_Operations_POC login =
            new QC_Operations_POC();

        // Use the writing example to generate an entity so that we
        // can read it.
        // Go over this code to learn how to create new entities.
        CreateDeleteExample writeExample = new CreateDeleteExample();

        boolean loginState = login.login(username, password);
        
        System.out.println("Loggin result : " + loginState);

        //read a simple resource, not even an entity..
        //original
//       String urlOfResourceWeWantToRead = con.buildUrl("rest/server");
        String urlOfResourceWeWantToRead = con.buildEntityCollectionUrl("test-folders/22613");
        
        Map<String, String> requestHeaders = new HashMap<>();
//        requestHeaders.put("Accept", "application/xml");
        requestHeaders.put("Accept", "application/json");
      
        Response serverResponse = con.httpGet(urlOfResourceWeWantToRead,null, requestHeaders);
//        Response serverResponse = con.httpGet(urlOfResourceWeWantToRead,"query={name['Root']}", requestHeaders);
        System.out.println("Status Code of Response: " + serverResponse.getStatusCode());
       
        Assert.assertEquals(
                "failed obtaining response for " + urlOfResourceWeWantToRead,
                serverResponse.getStatusCode(),
                HttpURLConnection.HTTP_OK);

        String responseStr = serverResponse.toString();
        System.out.println(responseStr);
        Assert.assertTrue(
                "server properties not found.",
                responseStr.contains("<ServerProperties>"));

        // After accessing a resource, a qc session is implicitly created.
        // The following code asserts this.
        Assert.assertTrue(
                "cookie string doesn't contain QCSession.",
                con.getCookieString().contains("QCSession"));

        String requirementsUrl = con.buildEntityCollectionUrl("requirement");

        // Use the writing example to generate an entity so that we can read it.
        // Go over this code to learn how to create new entities.
        String newCreatedResourceUrl =
                writeExample.createEntity(requirementsUrl,
                    Constants.entityToPostXml);

        //query a collection of entities:
        StringBuilder b = new StringBuilder();
        //The query - where field name has a  value that starts with the
        // name of the requirement we posted
        b.append("query={name[" + Constants.entityToPostName + "]}");
        //The fields to display: id, name
        b.append("&fields=id,name");
        //determine the sorting order - descending by id (highest id first)
        b.append("&order-by={id[DESC]}");
        //display 10 results
        b.append("&page-size=10");
        //counting from the 1st result, including
        b.append("&start-index=1");

        serverResponse =
            con.httpGet(requirementsUrl, b.toString(), requestHeaders);
        Assert.assertEquals(
                "failed obtaining response for requirements collection "
                + requirementsUrl,
                HttpURLConnection.HTTP_OK,
                serverResponse.getStatusCode());

        String listFromCollectionAsXml = serverResponse.toString();
        Assert.assertTrue(
"didn't find exactly one match, though we posted exactly one entity with '"
             + Constants.entityToPostName
             + "' name",
             listFromCollectionAsXml.contains("<Entities TotalResults=\"1\">"));

        // Read the entity we generated in the above step.
        // Just perform a get operation on its url.
        serverResponse =
            con.httpGet(newCreatedResourceUrl, null, requestHeaders);
        Assert.assertEquals(
                "failed obtaining response for requirements collection "
                + requirementsUrl,
                HttpURLConnection.HTTP_OK,
                serverResponse.getStatusCode());

        //xml -> class instance
        String postedEntityReturnedXml = serverResponse.toString();
        Entity entity =
            EntityMarshallingUtils.marshal(Entity.class, postedEntityReturnedXml);

        //now show that you can do something with that object
        List<Field> fields = entity.getFields().getField();
        boolean encounteredPostedField = false;
        for (Field field : fields) {
            if (field.getName().equals(Constants.entityToPostFieldName)) {
                Assert.assertEquals(
                        Constants.entityToPostFieldName
                                + " differs from "
                                + Constants.entityToPostFieldValue
                                + ", though we set it to 1",
                        Constants.entityToPostFieldValue,
                        field.getValue().iterator().next());
                encounteredPostedField = true;
                break;
            }
        }

        Assert.assertTrue("read element didn't contain field '"
                          + Constants.entityToPostFieldName
                          + "' with value '"
                          + Constants.entityToPostFieldValue
                          + "', though it's part of the posted element.",
                          encounteredPostedField);

        //cleanup
        writeExample.deleteEntity(newCreatedResourceUrl);
        login.logout();
    }
}

