package example.springdata.cosmosdb;

import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentDbPageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public Page<User> getUsers(DocumentDbPageRequest pageable) {
        return userRepository.findAll(pageable);
    }
}
