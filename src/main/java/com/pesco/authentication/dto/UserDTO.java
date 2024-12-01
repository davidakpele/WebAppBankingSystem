package com.pesco.authentication.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.pesco.authentication.models.Users;
import java.util.stream.Collectors;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    @Id
    private Long id;
    private String email;
    @JsonIgnore
    private String password;
    private String username;
    @Column(name = "createdOn")
    private LocalDateTime createdOn;
    @Column(name = "updatedOn")
    private LocalDateTime updatedOn;
    private boolean enabled;
    @JsonManagedReference
    private List<UserRecordDTO> records;

    public static UserDTO fromEntity(Users user) {
        UserDTO userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setEnabled(user.isEnabled());
        userDto.setCreatedOn(user.getCreatedOn());
        List<UserRecordDTO> recordsDTOs = user.getRecords()
                .stream()
                .map(UserRecordDTO::fromEntity)
                .collect(Collectors.toList());
        userDto.setRecords(recordsDTOs);
        return userDto;
    }
}
