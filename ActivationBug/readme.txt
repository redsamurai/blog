1 This application uses HR schema, more specifically REGIONS table.
2.To reproduce the problem, you will need to follow the specific steps:
2.1. Generate a large number of records into your REGIONS table. For that you can use BC tester and run AmModule's insert() method.
     Adjust the method and repeat the procedure until you generate around half of millions of records.
2.2. Make sure you set the set appropriate logging in the Weblogic, 'com.redsamurai' -> FINEST.
2.2. Run view1.jspx page.
2.2.1. When page opens, press 'CreateInsert' button, then press 'open' button. This should bring an empty form.
2.2.2. Provide values for 'RegionId' and 'RegionName'.
2.2.3. Click 'Commit' button.

At this point you will see in the console logger how all the records are fetched one-by-one, and if the number of records in your table is large enougn, it will generate OutOfMemoryError. 


 