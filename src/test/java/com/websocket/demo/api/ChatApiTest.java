package com.websocket.demo.api;

import com.websocket.demo.controller.RestDocs;
import com.websocket.demo.domain.Chat;
import com.websocket.demo.domain.Room;
import com.websocket.demo.request.CreateRoomRequest;
import com.websocket.demo.request.LoginRequest;
import com.websocket.demo.response.ChatInfo;
import com.websocket.demo.response.RoomInfo;
import com.websocket.demo.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatApiTest extends RestDocs {

    @MockBean
    ChatService chatService;

    @DisplayName("체팅방의 체팅 목록 조회 API")
    @Test
    public void getChattingList() throws Exception {
        //given
        var room = spy(Room.class);
        var result = ChatInfo.from(createChat(1L, "nickname", "contents..", room,
                LocalDateTime.of(1999, 10, 10, 12, 10, 10)));
        given(chatService.findChatList(any())).willReturn(List.of(result));
        given(room.getId()).willReturn(100L);
        //when then
        mockMvc.perform(get("/chat")
                        .queryParam("roomId", "100")
                        .queryParam("from", "1999-10-10T00:00:00")
                        .queryParam("to", "1999-10-11T00:00:00")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpectAll(
                        jsonPath("$.status").value("success"),
                        jsonPath("$.data[0].id").value(result.getId()),
                        jsonPath("$.data[0].sender").value(result.getSender()),
                        jsonPath("$.data[0].message").value(result.getMessage()),
                        jsonPath("$.data[0].roomId").value(result.getRoomId()),
                        jsonPath("$.data[0].createdAt").exists()
                )
                .andDo(
                        document("get-chatList",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                queryParameters(
                                        parameterWithName("roomId").description("채팅방 id"),
                                        parameterWithName("from").description("검색 조건 : 체팅 생성 구간 from (format=yyyy-MM-dd)"),
                                        parameterWithName("to").description("검색 조건 : 체팅 생성 구간 to (format=yyyy-MM-dd)")
                                ),
                                responseFields(
                                        fieldWithPath("status").description("요청 처리 결과"),
                                        fieldWithPath("data[].id").description("채팅 id"),
                                        fieldWithPath("data[].sender").description("발신자"),
                                        fieldWithPath("data[].message").description("채팅 내용"),
                                        fieldWithPath("data[].roomId").description("채킹방 id"),
                                        fieldWithPath("data[].createdAt").description("생성시간")
                                )
                        )
                );
    }

    @DisplayName("체팅방 생성 API")
    @Test
    public void createRoom() throws Exception {
        //given
        var request = new CreateRoomRequest();
        request.setTitle("room title");
        request.setUsers(List.of("user1", "user2", "user3"));
        var response = new RoomInfo(
                100L,
                "room title",
                List.of("user1", "user2", "user3"),
                List.of()
        );
        given(chatService.createRoom(request)).willReturn(response);
        //when //then
        String content = mapper.writeValueAsString(request);
        mockMvc.perform(MockMvcRequestBuilders.post("/room")
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value("success"),
                        jsonPath("$.data.id").exists(),
                        jsonPath("$.data.title").exists(),
                        jsonPath("$.data.users").exists(),
                        jsonPath("$.data.chat").exists()
                ).andDo(
                        document("post-create-room",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("title").description("채팅방 이름"),
                                        fieldWithPath("users").description("채팅방 구성원 닉네임 목록으로 반드시 하나 이상의 닉네임을 포함해야한다.")
                                ),
                                responseFields(
                                        fieldWithPath("status").description("요청 처리 결과"),
                                        fieldWithPath("data.id").description("채팅방 id"),
                                        fieldWithPath("data.title").description("채팅방 이름"),
                                        fieldWithPath("data.users").description("채팅방 구성원 닉네임 목록"),
                                        fieldWithPath("data.chat").description("채팅 목록")
                                )
                        )
                );
    }

    @DisplayName("채팅방 조회 API")
    @Test
    public void getRoomList() throws Exception {
        //given
        var info = new LoginRequest();
        info.setNickname("nickname");
        var chatInfo = new ChatInfo();
        chatInfo.setId(1L);
        chatInfo.setRoomId(100L);
        chatInfo.setSender("john");
        chatInfo.setMessage("hello");
        chatInfo.setCreatedAt(LocalDateTime.of(2000, 12, 12, 12, 12, 12));

        var roomInfo = new RoomInfo(100L, "room title", List.of(), List.of(chatInfo));
        given(chatService.findRoomList("nickname")).willReturn(List.of(roomInfo));
        //when then
        mockMvc.perform(get("/room").sessionAttr("user", info))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value("success"),
                        jsonPath("$.data[0].id").value(roomInfo.getId()),
                        jsonPath("$.data[0].title").value(roomInfo.getTitle()),
                        jsonPath("$.data[0].chat[0].id").value(chatInfo.getId()),
                        jsonPath("$.data[0].chat[0].sender").value(chatInfo.getSender()),
                        jsonPath("$.data[0].chat[0].message").value(chatInfo.getMessage()),
                        jsonPath("$.data[0].chat[0].roomId").value(chatInfo.getRoomId()),
                        jsonPath("$.data[0].chat[0].createdAt").exists()
                ).andDo(
                        document("get-room-list",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("status").description("요청 처리 결과"),
                                        fieldWithPath("data[].id").description("채팅방 id"),
                                        fieldWithPath("data[].title").description("채팅방 제목"),
                                        fieldWithPath("data[].chat[].id").description("채팅 id"),
                                        fieldWithPath("data[].users").description("채팅방 유저 닉네임 목록"),
                                        fieldWithPath("data[].chat[].sender").description("발신자"),
                                        fieldWithPath("data[].chat[].message").description("채팅 내용"),
                                        fieldWithPath("data[].chat[].roomId").description("채킹방 id"),
                                        fieldWithPath("data[].chat[].createdAt").description("채팅 생성시간")
                                )
                        )
                );
    }

    private Chat createChat(long id, String nickname, String message, Room room, LocalDateTime createdAt) {
        Chat chat = spy(Chat.builder()
                .room(room)
                .message(message)
                .senderNickname(nickname)
                .build());
        given(chat.getId()).willReturn(id);
        given(chat.getCreatedAt()).willReturn(createdAt);
        return chat;
    }
}