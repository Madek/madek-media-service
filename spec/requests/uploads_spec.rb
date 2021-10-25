require "requests/uploads/shared/configuration"
require "requests/shared/authorization_error"

describe "Uploads", type: :request do
  include_context "configuration"

  describe "GET: uploads/" do
    let(:response) { faraday_client_with_token.get("uploads/") }

    context "with public access" do
      let(:user) { nil }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      it "responds with success" do
        expect(response.status).to eq(200)
      end
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      it "responds with success" do
        expect(response.status).to eq(200)
      end
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }

      it "responds with success" do
        expect(response.status).to eq(200)
      end
    end
  end

  describe "POST: uploads/" do
    let!(:store) { create(:media_store, :database, :with_users, users: [user].compact) }
    let(:request) do
      faraday_client_with_token.post("uploads/",
                                content_type: "text/plain",
                                filename: "small.txt",
                                md5: md5,
                                media_store_id: store.id,
                                size: file_size)
    end
    let(:response) { request }
    let(:parsed_body) { JSON.parse(response.body) }

    shared_examples "successful response" do
      it "responds with success" do
        expect(response.status).to eq(200)
      end

      it "returns upload details" do
        expect(without_timestamps(parsed_body))
          .to match({
            id: a_kind_of(String),
            md5: md5,
            state: "announced",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            media_file_id: nil,
            error: nil,
            sha256: nil,
          }.stringify_keys)
      end
    end

    context "with public access" do
      let(:user) { nil }

      it_raises "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      include_examples "successful response"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      include_examples "successful response"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }

      include_examples "successful response"
    end
  end
end
