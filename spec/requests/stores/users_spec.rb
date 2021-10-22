require 'requests/shared/system_admin_error'

describe "Resources" do
  describe "Users: /media-service/stores/:store_id/users/", type: :request do
    let(:api_token) { create(:api_token, user: user, scope_write: true) }
    let(:user_token) { api_token.token_hash }
    let(:request) { faraday_client_with_token.get("stores/#{store_id}/users/") }
    let(:response) { request }
    let(:store) { create(:media_store) }
    let(:store_id) { store.id }

    context "with public access" do
      let(:user) { nil }

      it_raises "system admin error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      it_raises "system admin error"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      it_raises "system admin error"
    end

    context "for user with system admin role" do
      let(:person) { create(:person, first_name: "Richie", last_name: "Horn") }
      let!(:user) { create(:user, :with_system_admin_role, person: person) }
      let!(:another_store) { create(:media_store) }
      let(:person_2) { create(:person, first_name: "Liana", last_name: "Warner") }
      let(:person_3) { create(:person, first_name: "Clara", last_name: "Warner") }
      let(:person_4) { create(:person, first_name: "Andy", last_name: "Zulu") }
      let(:user_2) { create(:user, person: person_2) }
      let(:user_3) { create(:user, person: person_3) }
      let!(:user_4) { create(:user, person: person_4) }
      let(:group) { create(:group) }

      before do
        group.users << user_3
        store.users << [user_2, user_3]
        store.groups << group

        store.media_store_users.find_by(user: user_2).update_attribute(:priority, 7)
        store.media_store_users.find_by(user: user_3).update_attribute(:priority, 4)
        store.media_store_groups.find_by(group: group).update_attribute(:priority, 3)
      end

      describe "listing" do
        let(:parsed_body) { JSON.parse(response.body) }

        it "responses with success" do
          expect(response.status).to eq(200)
        end

        it "returns users for the store" do
          expect(parsed_body).to eq({
            users: [
              {
                email: user.email,
                first_name: user.person.first_name,
                last_name: user.person.last_name,
                person_id: user.person_id,
                user_id: user.id,
                key: user.id,
                direct_priority: nil,
                groups_priority: nil,
                index: 0,
                "page-index": 0
              }, {
                email: user_3.email,
                first_name: user_3.person.first_name,
                last_name: user_3.person.last_name,
                person_id: user_3.person_id,
                user_id: user_3.id,
                key: user_3.id,
                direct_priority: 4,
                groups_priority: 3,
                index: 1,
                "page-index": 1
              }, {
                email: user_2.email,
                first_name: user_2.person.first_name,
                last_name: user_2.person.last_name,
                person_id: user_2.person_id,
                user_id: user_2.id,
                key: user_2.id,
                direct_priority: 7,
                groups_priority: nil,
                index: 2,
                "page-index": 2
              }, {
                email: user_4.email,
                first_name: user_4.person.first_name,
                last_name: user_4.person.last_name,
                person_id: user_4.person_id,
                user_id: user_4.id,
                key: user_4.id,
                direct_priority: nil,
                groups_priority: nil,
                index: 3,
                "page-index": 3
              }
            ]
          }.deep_stringify_keys)
        end

        describe "filtering" do
          let(:response) do
            faraday_client_with_token.get("stores/#{store_id}/users/?term=horn")
          end

          it "responses with success" do
            expect(response.status).to eq(200)
          end

          it "returns filtered users" do
            expect(parsed_body).to eq({
              users: [
                {
                  email: user.email,
                  first_name: user.person.first_name,
                  last_name: user.person.last_name,
                  person_id: user.person_id,
                  user_id: user.id,
                  key: user.id,
                  direct_priority: nil,
                  groups_priority: nil,
                  index: 0,
                  "page-index": 0
                }
              ]
            }.deep_stringify_keys)
          end
        end
      end

      describe "updating user's priority" do
        let(:perform_request) do
          faraday_client_with_token
            .put("stores/#{store_id}/users/#{user_2.id}/direct-priority",
                 priority: 4)
        end

        it "changes user's priority" do
          expect { perform_request }
            .to change { store.media_store_users.find_by(user: user_2).priority }
            .from(7).to(4)
        end
      end

      describe "deleting user's priority" do
        let(:perform_request) do
          faraday_client_with_token
            .delete("stores/#{store_id}/users/#{user_2.id}/direct-priority")
        end

        it "responds with 204" do
          expect(perform_request.status).to eq(204)
        end

        it "deletes user's priority" do
          expect { perform_request }
            .to change { store.media_store_users.where(user: user_2).count }
            .from(1).to(0)
        end
      end
    end
  end
end
