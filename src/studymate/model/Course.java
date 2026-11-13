 package studymate.model;                               
                                                        
 import java.util.UUID;                                 
                                                        
 public class Course {                                  
                                                        
     private UUID id;                                   
     private String name;                               
                                                        
     // Used when creating a new course                 
     public Course(String name) {                       
         this.id = UUID.randomUUID();                   
         this.name = name;                              
     }                                                  
                                                        
     // Used when loading an existing course from SQLite
     public Course(UUID id, String name) {              
         this.id = id;                                  
         this.name = name;                              
     }                                                  
                                                        
     public UUID id() {                                 
         return id;                                     
     }                                                  
                                                        
     public String getName() {                          
         return name;                                   
     }                                                  
 }                                                      