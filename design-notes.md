#Ranking
- I chose to create a ranking based on distance, price, then listing date freshness. 
- The ranking is shown in the listing serivce output for validation purposes. 
- Before ranking I organized the listings by status to display all the active statuses before pending or sold. 

#Listing Injestion Logging
- I chose to spend a little extra time to enable selection of the example json file which is then validated, logged and saved. 
- My experience with building service applications like this is that the easier you make them to test, the better the result. 
- This allows developers and testors to share listing files in the early stages of development, before the infrastruture is in place that would handle this in production. 
- The process also allows the listings to be updated with the most current file by useing a generated key value based on source and id. 
- This would be simlar to how the listings would be handled if a messaging or event driven system was the source instead of an upload. 

#Timebox
- I did the minimum required on the UI. Just enough to demonstrate each flow.  
- I focused on creating a detailed design up front that I could then use to create the build spec.
- I started with the UX flow to ensure the steps were handled correctly, then designed the API's needed. 

#Tools
- I used Claude to validate my design and create the final build spec. 



